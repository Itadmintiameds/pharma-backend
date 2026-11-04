package tiameds.pharmabackend.service.product.bulk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.PackagingDetailsDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.bulk.BulkUploadRowResult;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.product.ProductMapper;
import tiameds.pharmabackend.mapper.product.category.ProductInventoryMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes one product's worth of CSV lines — the product, its packages, its batches
 * and their opening stock — inside a single transaction.
 * <p>
 * The unit of work is deliberately the product, not the file: one bad product rolls
 * back only its own lines, and the rest of the upload still lands. That is also why
 * this sits in its own bean, so the orchestrator's call goes through the transactional
 * proxy.
 * <p>
 * The write is idempotent at batch level. A product the organization already has is
 * reused rather than duplicated, a package with the same unit and pack size is reused,
 * and a batch already on record is skipped — including its stock, so re-running the
 * same file does not inflate inventory.
 */
@Service
@RequiredArgsConstructor
public class ProductBulkGroupWriter {

    private final ProductDetailsRepository productRepo;
    private final PackagingDetailsRepository packagingRepo;
    private final BatchDetailsRepository batchRepo;
    private final PharmacyDetailsRepository pharmacyRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductMapper productMapper;
    private final ProductInventoryMapper inventoryMapper;
    private final List<InventoryAdjuster> adjusters;

    /**
     * @param group every line sharing one product code, in file order
     * @return one result per line of the group
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<BulkUploadRowResult> write(List<MappedRow> group, BulkUploadTarget target, String actor) {
        LocalDateTime now = LocalDateTime.now();
        MappedRow first = group.get(0);

        PharmacyDetails pharmacy = null;
        Warehouse warehouse = null;
        PharmacyOrganization organization;
        if (target.isWarehouse()) {
            warehouse = warehouseRepo.findById(target.locationId())
                    .orElseThrow(() -> new IllegalStateException("Warehouse not found: " + target.locationId()));
            organization = warehouse.getOrganization();
        } else {
            pharmacy = pharmacyRepo.findById(target.locationId())
                    .orElseThrow(() -> new IllegalStateException("Pharmacy not found: " + target.locationId()));
            organization = pharmacy.getOrganization();
        }
        if (organization == null) {
            throw new IllegalStateException("Location is not linked to an organization; cannot import products");
        }

        ProductDetails existing = productRepo
                .findByOrganization_OrganizationIdAndProductNameAndBrandNameAndHsnNo(
                        organization.getOrganizationId(),
                        first.getProductName(), first.getBrandName(), first.getHsnNo())
                .orElse(null);

        boolean created = existing == null;
        ProductDetails product = created
                ? createProduct(first, organization, target, actor, now)
                : existing;

        mapToLocation(product, pharmacy, warehouse);

        // Reserve contiguous id blocks up front: the max-id queries only see rows that
        // are already persisted, so generating one at a time inside a transaction would
        // hand out the same id twice.
        Map<String, PackagingDetails> packagesByKey = indexExistingPackages(product);
        int newPackageCount = (int) group.stream()
                .map(MappedRow::packagingKey)
                .distinct()
                .filter(key -> !packagesByKey.containsKey(key))
                .count();
        List<String> packagingIds = nextPackagingIds(target.locationName(), newPackageCount);
        int nextPackagingId = 0;

        List<String> batchIds = nextBatchIds(target.locationName(), group.size());
        int nextBatchId = 0;

        List<BulkUploadRowResult> results = new ArrayList<>();
        List<PendingStock> pendingStock = new ArrayList<>();

        for (MappedRow row : group) {
            BulkUploadRowResult result = new BulkUploadRowResult();
            result.setLine(row.getLine());
            result.setProductCode(row.getProductCode());
            result.setProductName(row.getProductName());
            result.setBatchNumber(row.getBatch().getBatchNumber());
            result.setProductId(product.getProductId());
            result.getWarnings().addAll(row.getWarnings());
            results.add(result);

            PackagingDetails packaging = packagesByKey.get(row.packagingKey());
            if (packaging == null) {
                packaging = createPackage(row, product, packagingIds.get(nextPackagingId++), actor, now);
                packagesByKey.put(row.packagingKey(), packaging);
            }
            result.setPackagingId(packaging.getPackagingId());

            BatchDetails batch = findExistingBatch(product, packaging, row.getBatch().getBatchNumber());
            if (batch != null) {
                result.setBatchId(batch.getBatchId());
                result.setStatus(BulkUploadRowResult.Status.SKIPPED);
                result.setError("Batch \"" + batch.getBatchNumber()
                        + "\" is already on this package — left unchanged, stock not re-added");
                continue;
            }

            batch = createBatch(row.getBatch(), product, packaging, batchIds.get(nextBatchId++), actor, now);
            result.setBatchId(batch.getBatchId());
            result.setStatus(created
                    ? BulkUploadRowResult.Status.CREATED
                    : BulkUploadRowResult.Status.UPDATED);

            Long stock = row.getStockInSmallestUnits();
            if (stock != null && stock > 0) {
                pendingStock.add(new PendingStock(packaging, batch, stock, result));
            }
        }

        // One save cascades the product, its packages and their batches.
        productRepo.saveAndFlush(product);

        InventoryAdjuster adjuster = adjusterFor(target);
        for (PendingStock stock : pendingStock) {
            adjuster.increment(new StockAdjustment(
                    target.locationId(), product, stock.packaging(), stock.batch(), stock.quantity(),
                    TransactionType.STOCK_ADJUSTMENT, null, null, actor, now));
            stock.result().setStockAdded(stock.quantity());
        }

        return results;
    }

    // ===== product =====

    private ProductDetails createProduct(MappedRow row, PharmacyOrganization organization,
                                         BulkUploadTarget target, String actor, LocalDateTime now) {
        // Scalars only: packages and batches are attached per line below, because a
        // product's lines may describe more than one pack size.
        ProductDetailsDto dto = new ProductDetailsDto();
        dto.setProductName(row.getProductName());
        dto.setBrandName(row.getBrandName());
        dto.setHsnNo(row.getHsnNo());
        dto.setGstPercentage(row.getGstPercentage());
        dto.setProductCategoryId(row.getProductCategoryId());

        String productId = generateProductId(row.getProductName(), target.locationName());
        ProductDetails product = productMapper.toEntity(dto, productId, actor, now);
        product.setOrganization(organization);

        // applyAttributeUpdates assigns the deterministic per-product attribute ids
        // (_DRUG_n, _COSM, ...) and the molecule composite keys, the same way onboarding does.
        ProductDetailsDto attributes = new ProductDetailsDto();
        if (row.getDrug() != null) {
            attributes.setProductAttributeDrugs(List.of(row.getDrug()));
        }
        if (row.getCosmetics() != null) {
            attributes.setProductAttributeCosmetics(List.of(row.getCosmetics()));
        }
        if (row.getSupplements() != null) {
            attributes.setProductAttributeSupplements(List.of(row.getSupplements()));
        }
        if (row.getFoodInfant() != null) {
            attributes.setProductAttributeFoodInfants(List.of(row.getFoodInfant()));
        }
        if (row.getConsumable() != null) {
            attributes.setProductAttributeConsumableMedicals(List.of(row.getConsumable()));
        }
        if (row.getNonConsumable() != null) {
            attributes.setProductAttributeNonConsumableMedicals(List.of(row.getNonConsumable()));
        }
        productMapper.applyAttributeUpdates(product, attributes, actor, now);

        if (product.getPackagingDetails() == null) {
            product.setPackagingDetails(new ArrayList<>());
        }
        if (product.getBatchDetails() == null) {
            product.setBatchDetails(new ArrayList<>());
        }
        return product;
    }

    private void mapToLocation(ProductDetails product, PharmacyDetails pharmacy, Warehouse warehouse) {
        if (warehouse != null) {
            if (product.getWarehouses() == null) {
                product.setWarehouses(new ArrayList<>());
            }
            boolean mapped = product.getWarehouses().stream()
                    .anyMatch(w -> warehouse.getWarehouseId().equals(w.getWarehouseId()));
            if (!mapped) {
                product.getWarehouses().add(warehouse);
            }
            return;
        }
        if (product.getPharmacies() == null) {
            product.setPharmacies(new ArrayList<>());
        }
        boolean mapped = product.getPharmacies().stream()
                .anyMatch(p -> pharmacy.getPharmacyId().equals(p.getPharmacyId()));
        if (!mapped) {
            product.getPharmacies().add(pharmacy);
        }
    }

    // ===== packaging =====

    /**
     * Existing packages keyed the same way {@link MappedRow#packagingKey()} keys a line,
     * so a repeat of a pack size reuses the package instead of adding another.
     */
    private Map<String, PackagingDetails> indexExistingPackages(ProductDetails product) {
        Map<String, PackagingDetails> byKey = new LinkedHashMap<>();
        if (product.getPackagingDetails() == null) {
            return byKey;
        }
        for (PackagingDetails packaging : product.getPackagingDetails()) {
            Long unitId = packaging.getPurchaseSmallestUnit() == null
                    ? null
                    : packaging.getPurchaseSmallestUnit().getPurchaseSmallestUnitId();
            byKey.putIfAbsent(unitId + "/" + packaging.getPurchaseUnitContains(), packaging);
        }
        return byKey;
    }

    private PackagingDetails createPackage(MappedRow row, ProductDetails product,
                                           String packagingId, String actor, LocalDateTime now) {
        PackagingDetailsDto dto = new PackagingDetailsDto();
        dto.setPurchaseUnitContains(row.getPurchaseUnitContains());
        dto.setPurchaseSmallestUnitId(row.getPurchaseSmallestUnitId());

        // toEntity resolves the unit master and derives purchaseUnit from it.
        PackagingDetails packaging = inventoryMapper.toEntity(dto, actor, now);
        packaging.setPackagingId(packagingId);
        packaging.setProduct(product);
        if (packaging.getBatchDetails() == null) {
            packaging.setBatchDetails(new ArrayList<>());
        }
        product.getPackagingDetails().add(packaging);
        return packaging;
    }

    // ===== batches =====

    private BatchDetails findExistingBatch(ProductDetails product, PackagingDetails packaging,
                                           String batchNumber) {
        // A package created moments ago in this same transaction has no batches on record.
        if (packaging.getBatchDetails() != null) {
            for (BatchDetails batch : packaging.getBatchDetails()) {
                if (batchNumber.equalsIgnoreCase(batch.getBatchNumber())) {
                    return batch;
                }
            }
        }
        if (product.getProductId() == null || packaging.getPackagingId() == null) {
            return null;
        }
        return batchRepo.findByBatchNumberAndProduct_ProductIdAndPackagingDetails_PackagingId(
                batchNumber, product.getProductId(), packaging.getPackagingId()).orElse(null);
    }

    private BatchDetails createBatch(BatchDetailsDto dto, ProductDetails product,
                                     PackagingDetails packaging, String batchId,
                                     String actor, LocalDateTime now) {
        BatchDetails batch = inventoryMapper.toEntity(dto, actor, now);
        batch.setBatchId(batchId);
        batch.setProduct(product);
        batch.setPackagingDetails(packaging);
        packaging.getBatchDetails().add(batch);
        product.getBatchDetails().add(batch);
        return batch;
    }

    // ===== stock =====

    private InventoryAdjuster adjusterFor(BulkUploadTarget target) {
        return adjusters.stream()
                .filter(a -> a.locationType() == target.type())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No inventory adjuster for location type " + target.type()));
    }

    /**
     * A batch whose opening stock is applied once the product has been flushed.
     */
    private record PendingStock(PackagingDetails packaging, BatchDetails batch, long quantity,
                                BulkUploadRowResult result) {
    }

    // ===== id generation =====
    // Mirrors ProductServiceImpl's generators so bulk-created ids are indistinguishable
    // from ids created through the onboarding endpoint.

    private String locationPrefix(String locationName) {
        String cleaned = (locationName == null ? "" : locationName).replaceAll("[^a-zA-Z]", "").toUpperCase();
        return cleaned.length() >= 2
                ? cleaned.substring(0, 2)
                : String.format("%-2s", cleaned).replace(' ', 'X');
    }

    private synchronized String generateProductId(String productName, String locationName) {
        String namePart = (productName == null ? "" : productName).replaceAll("[^a-zA-Z]", "").toUpperCase();
        namePart = namePart.length() >= 3
                ? namePart.substring(0, 3)
                : String.format("%-3s", namePart).replace(' ', 'X');

        Integer lastNumber = productRepo.findMaxProductNumber();
        int nextNumber = (lastNumber == null) ? 1 : lastNumber + 1;
        return locationPrefix(locationName) + namePart + String.format("%05d", nextNumber);
    }

    private synchronized List<String> nextPackagingIds(String locationName, int count) {
        List<String> ids = new ArrayList<>();
        if (count <= 0) {
            return ids;
        }
        String prefix = locationPrefix(locationName);
        Integer lastNumber = packagingRepo.findMaxPackagingNumber();
        int next = (lastNumber == null) ? 0 : lastNumber;
        for (int i = 0; i < count; i++) {
            ids.add(prefix + "PKG" + String.format("%05d", ++next));
        }
        return ids;
    }

    private synchronized List<String> nextBatchIds(String locationName, int count) {
        List<String> ids = new ArrayList<>();
        if (count <= 0) {
            return ids;
        }
        String prefix = locationPrefix(locationName);
        Integer lastNumber = batchRepo.findMaxBatchNumber();
        int next = (lastNumber == null) ? 0 : lastNumber;
        for (int i = 0; i < count; i++) {
            ids.add(prefix + "BTCH" + String.format("%05d", ++next));
        }
        return ids;
    }
}
