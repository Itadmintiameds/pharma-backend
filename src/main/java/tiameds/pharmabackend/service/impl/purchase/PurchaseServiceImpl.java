package tiameds.pharmabackend.service.impl.purchase;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.purchase.PurchaseMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryAuditRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.supplier.SupplierMasterRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.impl.warehouse.stock.InventoryAdjusters;
import tiameds.pharmabackend.service.purchase.PurchaseService;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final ProductDetailsRepository pharmaProductDetailsRepository;
    private final BatchDetailsRepository pharmaBatchDetailsRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAuditRepository inventoryAuditRepository;
    private final LocationContextResolver locationContextResolver;
    private final InventoryAdjusters adjusters;
    private final WarehouseRepository warehouseRepository;


    @Override
    public PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // One decision point: a warehouse manager buys into their warehouse,
        // everyone else buys into their selected pharmacy.
        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return createWarehousePurchase(purchaseDto, persistentUser, location.getLocationId());
        }

        return createPharmacyPurchase(purchaseDto, persistentUser, location.getLocationId());
    }

    private PurchaseDto createPharmacyPurchase(
            PurchaseDto purchaseDto,
            UserDetails persistentUser,
            String pharmacyId) {

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        PharmacyDetails pharmacy = pharmacyDetailsRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Purchase purchase = PurchaseMapper.toEntity(purchaseDto);

        purchase.setPharmacyId(pharmacyId);

        SupplierMaster supplier = supplierMasterRepository.findById(purchaseDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        purchase.setSupplier(supplier);

        if (purchase.getPurchaseDetails() != null) {

            for (int i = 0; i < purchase.getPurchaseDetails().size(); i++) {

                PurchaseDetails detail = purchase.getPurchaseDetails().get(i);
                var dto = purchaseDto.getPurchaseDetails().get(i);

                ProductDetails product = pharmaProductDetailsRepository
                        .findById(dto.getProductId())
                        .orElseThrow(() ->
                                new RuntimeException("Product not found: " + dto.getProductId()));

                BatchDetails batch = pharmaBatchDetailsRepository
                        .findById(dto.getBatchId())
                        .orElseThrow(() ->
                                new RuntimeException("Batch not found: " + dto.getBatchId()));

                detail.setProduct(product);
                detail.setBatch(batch);

            }
        }

        purchase.setGrnNo(generateGrnNo(pharmacyId));
        purchase.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        purchase.setCreatedAt(LocalDateTime.now());
        purchase.setModifiedBy(null);
        purchase.setModifiedAt(null);

        if (purchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                detail.setPurchase(purchase);
                detail.setCreatedBy(String.valueOf(persistentUser.getUserId()));
                detail.setCreatedAt(LocalDateTime.now());
                detail.setModifiedBy(null);
                detail.setModifiedAt(null);
            }
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);

        if (savedPurchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : savedPurchase.getPurchaseDetails()) {

                ProductDetails product = detail.getProduct();
                BatchDetails batch = detail.getBatch();
                PackagingDetails packaging = batch.getPackagingDetails();

                Long purchaseQty = detail.getPurchaseQuantity() != null
                        ? detail.getPurchaseQuantity()
                        : 0L;

                // Free units are supplied in the same unit as the purchase
                // quantity, so they convert the same way.
                Long freeQty = detail.getFreeQuantity() != null
                        ? detail.getFreeQuantity()
                        : 0L;

                Long purchaseUnitContains = (packaging != null
                        && packaging.getPurchaseUnitContains() != null)
                        ? packaging.getPurchaseUnitContains()
                        : 1L;

                // Stock in smallest units, free stock included
                Long stockQty = (purchaseQty + freeQty) * purchaseUnitContains;

                Inventory inventory = inventoryRepository
                        .findByProductAndPackagingAndBatch(product, packaging, batch)
                        .orElse(null);

                if (inventory == null) {

                    inventory = new Inventory();

                    inventory.setPharmacy(pharmacy);
                    inventory.setProduct(product);
                    inventory.setPackaging(packaging);
                    inventory.setBatch(batch);

                    inventory.setTotalStock(stockQty);

                    inventory.setCreatedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setCreatedAt(LocalDateTime.now());
                    inventory.setModifiedBy(null);
                    inventory.setModifiedAt(null);

                } else {

                    // Populate pharmacy if older inventory records don't have it
                    if (inventory.getPharmacy() == null) {
                        inventory.setPharmacy(pharmacy);
                    }

                    Long currentStock = inventory.getTotalStock() != null
                            ? inventory.getTotalStock()
                            : 0L;

                    inventory.setTotalStock(currentStock + stockQty);

                    inventory.setModifiedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setModifiedAt(LocalDateTime.now());
                }

                inventory = inventoryRepository.save(inventory);


                InventoryAudit audit = new InventoryAudit();

                audit.setInventory(inventory);
                audit.setPharmacy(pharmacy);
                audit.setPurchaseDetails(detail);
                audit.setStockMovement(StockMovement.IN);
                audit.setTransactionType(TransactionType.PURCHASE);
                audit.setChangeStock(stockQty);
                audit.setRemainingStock(inventory.getTotalStock());
                audit.setChangedBy(String.valueOf(persistentUser.getUserId()));
                audit.setChangedAt(LocalDateTime.now());

                inventoryAuditRepository.save(audit);
            }
        }

        return PurchaseMapper.toDto(savedPurchase);
    }


    private PurchaseDto createWarehousePurchase(
            PurchaseDto purchaseDto, UserDetails persistentUser, String warehouseId) {

        LocalDateTime now = LocalDateTime.now();
        String actor = String.valueOf(persistentUser.getUserId());

        Purchase purchase = PurchaseMapper.toEntity(purchaseDto);

        // Warehouse purchase: stock lands in the warehouse, not a pharmacy.
        // The warehouse comes from the manager's own binding, so it is already authorized.
        purchase.setPharmacyId(null);
        purchase.setWarehouseId(warehouseId);

        SupplierMaster supplier = supplierMasterRepository.findById(purchaseDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        purchase.setSupplier(supplier);

        // Product -> warehouse mapping now happens in WarehouseInventoryAdjuster.increment
        // (every stock-in maps the product to its location), so the explicit mapping here
        // is redundant and left commented for reference.
        // Warehouse warehouse = warehouseRepository.getReferenceById(warehouseId);

        if (purchase.getPurchaseDetails() != null) {

            for (int i = 0; i < purchase.getPurchaseDetails().size(); i++) {

                PurchaseDetails detail = purchase.getPurchaseDetails().get(i);
                var dto = purchaseDto.getPurchaseDetails().get(i);

                ProductDetails product = pharmaProductDetailsRepository
                        .findById(dto.getProductId())
                        .orElseThrow(() ->
                                new RuntimeException("Product not found: " + dto.getProductId()));

                BatchDetails batch = pharmaBatchDetailsRepository
                        .findById(dto.getBatchId())
                        .orElseThrow(() ->
                                new RuntimeException("Batch not found: " + dto.getBatchId()));

                detail.setProduct(product);
                detail.setBatch(batch);

                // Product -> warehouse mapping now handled by the adjuster's increment leg:
                // boolean alreadyMapped = product.getWarehouses().stream()
                //         .anyMatch(w -> warehouseId.equals(w.getWarehouseId()));
                // if (!alreadyMapped) {
                //     product.getWarehouses().add(warehouse);
                // }
            }
        }

        purchase.setGrnNo(generateGrnNoForWarehouse(warehouseId));
        purchase.setCreatedBy(actor);
        purchase.setCreatedAt(now);
        purchase.setModifiedBy(null);
        purchase.setModifiedAt(null);

        if (purchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                detail.setPurchase(purchase);
                detail.setCreatedBy(actor);
                detail.setCreatedAt(now);
                detail.setModifiedBy(null);
                detail.setModifiedAt(null);
            }
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);

        // Same LocationType-keyed adjuster the distribution flow uses; here the
        // location is a warehouse and the movement is a PURCHASE (stock-in only).
        InventoryAdjuster warehouseAdjuster = adjusters.of(LocationType.WAREHOUSE);

        if (savedPurchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : savedPurchase.getPurchaseDetails()) {

                BatchDetails batch = detail.getBatch();
                PackagingDetails packaging = batch.getPackagingDetails();

                Long purchaseQty = detail.getPurchaseQuantity() != null
                        ? detail.getPurchaseQuantity()
                        : 0L;

                // Free units are supplied in the same unit as the purchase
                // quantity, so they convert the same way.
                Long freeQty = detail.getFreeQuantity() != null
                        ? detail.getFreeQuantity()
                        : 0L;

                Long purchaseUnitContains = (packaging != null
                        && packaging.getPurchaseUnitContains() != null)
                        ? packaging.getPurchaseUnitContains()
                        : 1L;

                // Stock in smallest units, free stock included
                Long stockQty = (purchaseQty + freeQty) * purchaseUnitContains;

                warehouseAdjuster.increment(new StockAdjustment(
                        warehouseId,
                        detail.getProduct(),
                        packaging,
                        batch,
                        stockQty,
                        TransactionType.PURCHASE,
                        null,      // no distribution line for a supplier purchase
                        detail,    // trace the stock-in back to this purchase line
                        actor,
                        now));
            }
        }

        return PurchaseMapper.toDto(savedPurchase);
    }


    @Override
    public List<PurchaseDto> getAllPurchases(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return purchaseRepository.findByWarehouseId(location.getLocationId())
                    .stream()
                    .map(PurchaseMapper::toDto)
                    .collect(Collectors.toList());
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return purchaseRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(PurchaseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkInvoiceExists(
            Long supplierId,
            String invoiceNo,
            Integer year,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // One decision point: a warehouse manager checks against their warehouse's
        // purchase book, everyone else against their selected pharmacy.
        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (supplierId == null) {
            throw new RuntimeException("Supplier is required");
        }

        if (invoiceNo == null || invoiceNo.isBlank()) {
            throw new RuntimeException("Invoice number is required");
        }

        if (year == null) {
            throw new RuntimeException("Invoice year is required");
        }

        if (location.isWarehouse()) {
            return purchaseRepository.existsBySupplierInvoiceNoAndYearForWarehouse(
                    location.getLocationId(),
                    supplierId,
                    invoiceNo.trim(),
                    year);
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return purchaseRepository.existsBySupplierInvoiceNoAndYear(
                pharmacyId,
                supplierId,
                invoiceNo.trim(),
                year);
    }


    private String generateGrnNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "GRN-" + year + "-";

        List<String> latest = purchaseRepository.findLatestGrn(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestGrn = latest.get(0);

            String numberPart = latestGrn.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private String generateGrnNoForWarehouse(String warehouseId) {

        int year = LocalDate.now().getYear();
        String prefix = "GRN-" + year + "-";

        List<String> latest = purchaseRepository.findLatestGrnByWarehouse(
                prefix,
                warehouseId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestGrn = latest.get(0);

            String numberPart = latestGrn.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }
}