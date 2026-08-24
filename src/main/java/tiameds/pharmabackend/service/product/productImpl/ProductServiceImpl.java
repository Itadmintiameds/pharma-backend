package tiameds.pharmabackend.service.product.productImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.product.*;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventory;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.exception.ResourceNotFoundException;
import tiameds.pharmabackend.mapper.product.ProductMapper;
import tiameds.pharmabackend.mapper.product.category.ProductInventoryMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseInventoryRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.PharmacyOrganizationService;
import tiameds.pharmabackend.service.product.ProductService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDetailsRepository productRepo;

    @Autowired
    private PackagingDetailsRepository packagingRepo;

    @Autowired
    private BatchDetailsRepository batchRepo;

    @Autowired
    private PharmacyDetailsRepository pharmacyRepo;

    @Autowired
    private ProductMapper mapper;

    @Autowired
    private ProductInventoryMapper inventoryMapper;

    @Autowired
    private InventoryRepository inventoryRepo;

    @Autowired
    private CurrentPharmacyContext pharmacyContext;

    @Autowired
    private LocationContextResolver locationContextResolver;

    @Autowired
    private WarehouseInventoryRepository warehouseInventoryRepo;

    @Autowired
    private WarehouseRepository warehouseRepo;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private PharmacyOrganizationService organizationService;

    // A batch is "near expiry" when it expires within this many days from today.
    private static final long NEAR_EXPIRY_DAYS = 30;

    @Override
    @Transactional
    public ProductDetailsDto onboardProduct(ProductDetailsDto dto) {

//        PharmacyDetails pharmacy = pharmacyRepo.findById(dto.getPharmacyId())
//                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        // OLD (pharmacy-only) onboarding — replaced by location-aware resolution below:
        // String pharmacyId = pharmacyContext.getCurrentPharmacy();
        // PharmacyDetails pharmacy = pharmacyRepo.findById(pharmacyId)
        //         .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        // String rawPharmacyName = pharmacy.getPharmacyName();
        // final String pharmacyName = (rawPharmacyName == null) ? "XX" : rawPharmacyName;
        // String productId = generateProductId(dto.getProductName(), pharmacyName);
        // ProductDetails product = mapper.toEntity(dto, productId, createdBy, LocalDateTime.now());
        // if (product.getPharmacies() == null) {
        //     product.setPharmacies(new ArrayList<>());
        // }
        // product.getPharmacies().add(pharmacy);

        // Onboard into whichever location the user operates on: a warehouse manager
        // maps the product to their warehouse, everyone else to the selected pharmacy.
        LocationContext loc = currentLocation();

        String createdBy = "System";
        String userId = getCurrentUserId();
        if (userId != null) {
            createdBy = userId;
        }

        PharmacyDetails pharmacy = null;
        Warehouse warehouse = null;
        String locationName;

        if (loc.isWarehouse()) {
            warehouse = warehouseRepo.findById(loc.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            locationName = warehouse.getWarehouseName();
        } else {
            pharmacy = pharmacyRepo.findById(loc.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
            locationName = pharmacy.getPharmacyName();
        }

        // Kept the variable name so the id generators below need no change.
        final String pharmacyName = (locationName == null) ? "XX" : locationName;

        String productId = generateProductId(dto.getProductName(), pharmacyName);

        ProductDetails product = mapper.toEntity(dto, productId, createdBy, LocalDateTime.now());

        // OLD single-pharmacy assignment: product.setPharmacy(pharmacy);
        // Product now maps to many pharmacies/warehouses via ManyToMany.
        if (loc.isWarehouse()) {
            product.getWarehouses().add(warehouse);
        } else {
            if (product.getPharmacies() == null) {
                product.setPharmacies(new ArrayList<>());
            }
            product.getPharmacies().add(pharmacy);
        }
        dto.setProductId(productId);

        // Generate IDs and set bidirectional relationships
        if (product.getPackagingDetails() != null && dto.getPackagingDetails() != null) {
            for (int i = 0; i < product.getPackagingDetails().size(); i++) {
                String pId = generatePackagingId(pharmacyName);
                product.getPackagingDetails().get(i).setPackagingId(pId);
                product.getPackagingDetails().get(i).setProduct(product);
                dto.getPackagingDetails().get(i).setPackagingId(pId);
            }
        }

        if (product.getBatchDetails() != null && dto.getBatchDetails() != null) {
            for (int i = 0; i < product.getBatchDetails().size(); i++) {
                String bId = generateBatchId(pharmacyName);
                var batchEntity = product.getBatchDetails().get(i);
                var batchDto = dto.getBatchDetails().get(i);
                batchEntity.setBatchId(bId);
                batchEntity.setProduct(product);
                batchDto.setBatchId(bId);

                // Automatically link batch to the packaging details created in this onboarding request
                if (product.getPackagingDetails() != null && !product.getPackagingDetails().isEmpty()) {
                    var pkgEntity = product.getPackagingDetails().get(0);
                    batchEntity.setPackagingDetails(pkgEntity);
                    batchDto.setPackagingId(pkgEntity.getPackagingId());
                    if (pkgEntity.getBatchDetails() != null) {
                        pkgEntity.getBatchDetails().add(batchEntity);
                    }
                }
            }
        }

        // Setup supplements relationship
        if (product.getProductAttributeSupplements() != null && dto.getProductAttributeSupplements() != null) {
            for (int i = 0; i < product.getProductAttributeSupplements().size(); i++) {
                String sId = productId + "_SUPP";
                product.getProductAttributeSupplements().get(i).setProductAttributeId(sId);
                product.getProductAttributeSupplements().get(i).setProduct(product);
                dto.getProductAttributeSupplements().get(i).setProductAttributeId(sId);
            }
        }

        // Setup cosmetics relationship
        if (product.getProductAttributeCosmetics() != null && dto.getProductAttributeCosmetics() != null) {
            for (int i = 0; i < product.getProductAttributeCosmetics().size(); i++) {
                String cId = productId + "_COSM";
                product.getProductAttributeCosmetics().get(i).setProductAttributeId(cId);
                product.getProductAttributeCosmetics().get(i).setProduct(product);
                dto.getProductAttributeCosmetics().get(i).setProductAttributeId(cId);
            }
        }

        // Setup food and infants relationship
        if (product.getProductAttributeFoodInfants() != null && dto.getProductAttributeFoodInfants() != null) {
            for (int i = 0; i < product.getProductAttributeFoodInfants().size(); i++) {
                String fId = productId + "_FOOD";
                product.getProductAttributeFoodInfants().get(i).setProductAttributeId(fId);
                product.getProductAttributeFoodInfants().get(i).setProduct(product);
                dto.getProductAttributeFoodInfants().get(i).setProductAttributeId(fId);
            }
        }


        // Setup consumable medical relationship
        if (product.getProductAttributeConsumableMedicals() != null && dto.getProductAttributeConsumableMedicals() != null) {
            for (int i = 0; i < product.getProductAttributeConsumableMedicals().size(); i++) {
                String cId = productId + "_CONS";
                product.getProductAttributeConsumableMedicals().get(i).setProductAttributeId(cId);
                product.getProductAttributeConsumableMedicals().get(i).setProduct(product);
                dto.getProductAttributeConsumableMedicals().get(i).setProductAttributeId(cId);
            }
        }

        // Setup non-consumable medical relationship
        if (product.getProductAttributeNonConsumableMedicals() != null && dto.getProductAttributeNonConsumableMedicals() != null) {
            for (int i = 0; i < product.getProductAttributeNonConsumableMedicals().size(); i++) {
                String ncId = productId + "_NCONS";
                product.getProductAttributeNonConsumableMedicals().get(i).setProductAttributeId(ncId);
                product.getProductAttributeNonConsumableMedicals().get(i).setProduct(product);
                dto.getProductAttributeNonConsumableMedicals().get(i).setProductAttributeId(ncId);
            }
        }

        // Setup drugs relationship
        if (product.getProductAttributeDrugs() != null && dto.getProductAttributeDrugs() != null) {
            int entityIndex = 0;
            for (var dDto : dto.getProductAttributeDrugs()) {
                String drugAttrId = productId + "_DRUG_" + (entityIndex + 1);
                var drugEntity = product.getProductAttributeDrugs().get(entityIndex);
                drugEntity.setProductAttributeId(drugAttrId);
                drugEntity.setProduct(product);
                dDto.setProductAttributeId(drugAttrId);

                if (drugEntity.getProductMolecules() != null) {
                    for (var molEntity : drugEntity.getProductMolecules()) {
                        tiameds.pharmabackend.entity.product.ProductMoleculeId molId = new tiameds.pharmabackend.entity.product.ProductMoleculeId();
                        molId.setProductAttributeId(drugAttrId);
                        if (molEntity.getMolecule() != null) {
                            molId.setMoleculeId(molEntity.getMolecule().getMoleculeId());
                        }
                        molEntity.setId(molId);
                        molEntity.setProductAttributeDrug(drugEntity);
                    }
                }

                if (dDto.getProductMolecules() != null) {
                    for (var mDto : dDto.getProductMolecules()) {
                        mDto.setProductAttributeId(drugAttrId);
                    }
                }
                entityIndex++;
            }
        }

        productRepo.save(product);
        // Return the persisted entity (not the request dto) so server-derived fields
        // like packaging.purchaseUnit and the linked PurchaseSmallestUnit names are reflected.
        return mapper.toDto(product);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public java.util.List<ProductDetailsDto> getAllProducts() {
//        Long userId = getCurrentUserId();
//
//        if (userId == null) {
//            return productRepo.findAll().stream()
//                    .map(mapper::toDto)
//                    .collect(java.util.stream.Collectors.toList());
//        }
//
//        java.util.List<String> allowedPharmacies = pharmacyRepo.findPharmacyIdsByUserId(userId);
//
//        return productRepo.findAll().stream()
//                .filter(p -> p.getPharmacy() != null && allowedPharmacies.contains(p.getPharmacy().getPharmacyId()))
//                .map(mapper::toDto)
//                .collect(java.util.stream.Collectors.toList());
//    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetailsDto> getAllProducts() {

        return productsForLocation(currentLocation())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ===== API 1: products of the current pharmacy with stock + expiry status =====
    @Override
    @Transactional(readOnly = true)
    public List<ProductStockSummaryDto> getProductStockSummaries() {

        LocationContext loc = currentLocation();

        // Stock lives in inventory rows (pharma_inventory or pharma_warehouse_inventory),
        // not on the batch -> load the location's stock rows once and group by product.
        Map<String, List<StockRow>> stockByProduct =
                stockRowsForLocation(loc)
                        .stream()
                        .filter(row -> row.product() != null)
                        .collect(Collectors.groupingBy(row -> row.product().getProductId()));

        return productsForLocation(loc)
                .stream()
                .map(product -> toStockSummary(
                        product,
                        stockByProduct.getOrDefault(product.getProductId(), List.of())))
                .collect(Collectors.toList());
    }

    private ProductStockSummaryDto toStockSummary(ProductDetails product, List<StockRow> inventories) {
        ProductStockSummaryDto dto = new ProductStockSummaryDto();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setBrandName(product.getBrandName());

        if (product.getProductCategory() != null) {
            dto.setProductCategoryId(product.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(product.getProductCategory().getProductCategoryName());
        }

        dto.setManufacturerName(resolveManufacturerName(product));

        LocalDate today = LocalDate.now();
        LocalDate nearExpiryCutoff = today.plusDays(NEAR_EXPIRY_DAYS);

        long totalStock = 0L;
        long active = 0L;
        long nearExpiry = 0L;
        long expired = 0L;
        LocalDate nearestExpiry = null;

        for (StockRow inv : inventories) {
            long stock = inv.totalStock();

            totalStock += stock;

            // status is counted only over stock rows that actually hold stock
            if (stock <= 0 || inv.batch() == null) {
                continue;
            }

            LocalDate expiry = inv.batch().getExpiryDate();
            if (expiry == null) {
                // no expiry recorded -> treat as active, ignore for nearest-expiry
                active++;
                continue;
            }

            if (nearestExpiry == null || expiry.isBefore(nearestExpiry)) {
                nearestExpiry = expiry;
            }

            if (expiry.isBefore(today)) {
                expired++;
            } else if (!expiry.isAfter(nearExpiryCutoff)) {
                // today <= expiry <= today + NEAR_EXPIRY_DAYS
                nearExpiry++;
            } else {
                active++;
            }
        }

        dto.setTotalStock(totalStock);
        dto.setActiveBatches(active);
        dto.setNearExpiryBatches(nearExpiry);
        dto.setExpiredBatches(expired);
        dto.setNearestExpiryDate(nearestExpiry);
        dto.setOverallStatus(resolveOverallStatus(active, nearExpiry, expired));

        return dto;
    }

    /**
     * The manufacturer is stored on the category-specific attribute row, not on the
     * product itself, so it is read from whichever attribute list is populated for
     * this product. Drugs have no manufacturer attribute, so they return null.
     */
    private String resolveManufacturerName(ProductDetails product) {

        if (product.getProductAttributeNonConsumableMedicals() != null) {
            for (var attr : product.getProductAttributeNonConsumableMedicals()) {
                if (attr.getManufacturerName() != null) {
                    return attr.getManufacturerName();
                }
            }
        }

        if (product.getProductAttributeConsumableMedicals() != null) {
            for (var attr : product.getProductAttributeConsumableMedicals()) {
                if (attr.getManufacturerName() != null) {
                    return attr.getManufacturerName();
                }
            }
        }

        if (product.getProductAttributeSupplements() != null) {
            for (var attr : product.getProductAttributeSupplements()) {
                if (attr.getManufacturerName() != null) {
                    return attr.getManufacturerName();
                }
            }
        }

        if (product.getProductAttributeFoodInfants() != null) {
            for (var attr : product.getProductAttributeFoodInfants()) {
                if (attr.getManufacturerName() != null) {
                    return attr.getManufacturerName();
                }
            }
        }

        if (product.getProductAttributeCosmetics() != null) {
            for (var attr : product.getProductAttributeCosmetics()) {
                if (attr.getManufacturerName() != null) {
                    return attr.getManufacturerName();
                }
            }
        }

        return null;
    }

    private String resolveOverallStatus(long active, long nearExpiry, long expired) {
        // OLD priority (expired-first): EXPIRED > NEAR_EXPIRY > ACTIVE > OUT_OF_STOCK
        // if (expired > 0) {
        //     return "EXPIRED";
        // }
        // if (nearExpiry > 0) {
        //     return "NEAR_EXPIRY";
        // }
        // if (active > 0) {
        //     return "ACTIVE";
        // }
        // return "OUT_OF_STOCK";

        // NEW priority: NEAR_EXPIRY > ACTIVE (Healthy) > EXPIRED > OUT_OF_STOCK
        if (nearExpiry > 0) {
            return "NEAR_EXPIRY";
        }
        if (active > 0) {
            return "ACTIVE";
        }
        if (expired > 0) {
            return "EXPIRED";
        }
        return "OUT_OF_STOCK";
    }

    // ===== Dashboard KPI: product counts bucketed by nearest in-stock expiry =====
    @Override
    @Transactional(readOnly = true)
    public ProductExpiryKpiDto getExpiryKpi() {

        LocationContext loc = currentLocation();

        // in-stock rows for the location, grouped by product
        Map<String, List<StockRow>> inventoryByProduct =
                stockRowsForLocation(loc)
                        .stream()
                        .filter(row -> row.product() != null)
                        .collect(Collectors.groupingBy(row -> row.product().getProductId()));

        List<ProductDetails> products = productsForLocation(loc);

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        LocalDate in60Days = today.plusDays(60);

        long expired = 0;
        long expiring0To30 = 0;
        long expiring31To60 = 0;
        long healthy = 0;

        for (ProductDetails product : products) {
            List<StockRow> inventories =
                    inventoryByProduct.getOrDefault(product.getProductId(), List.of());

            // OLD logic — classified each product by the single nearest (earliest)
            // in-stock expiry, which gave EXPIRED precedence over NEAR_EXPIRY:
            // LocalDate nearest = null;
            // boolean hasInStock = false;
            //
            // for (StockRow inv : inventories) {
            //     long stock = inv.totalStock();
            //     if (stock <= 0 || inv.batch() == null) {
            //         continue;
            //     }
            //     hasInStock = true;
            //
            //     LocalDate expiry = inv.batch().getExpiryDate();
            //     if (expiry == null) {
            //         continue; // no expiry -> does not affect nearest
            //     }
            //     if (nearest == null || expiry.isBefore(nearest)) {
            //         nearest = expiry;
            //     }
            // }
            //
            // // products with no in-stock batch count only towards the total
            // if (!hasInStock) {
            //     continue;
            // }
            //
            // // only null-expiry stock on hand -> treat as healthy
            // if (nearest == null) {
            //     healthy++;
            // } else if (nearest.isBefore(today)) {
            //     expired++;
            // } else if (!nearest.isAfter(in30Days)) {
            //     expiring0To30++;               // today .. today + 30
            // } else if (!nearest.isAfter(in60Days)) {
            //     expiring31To60++;              // today + 31 .. today + 60
            // } else {
            //     healthy++;                     // > today + 60
            // }

            // NEW logic — classify each product once by priority across its in-stock
            // batches: NEAR_EXPIRY > HEALTHY (Active) > EXPIRED > OUT_OF_STOCK.
            boolean hasInStock = false;
            boolean hasHealthy = false;
            boolean hasExpired = false;
            // nearest expiry among the product's near-expiry batches; drives the
            // 0-30 vs 31-60 sub-bucket once NEAR_EXPIRY wins.
            LocalDate nearestNearExpiry = null;

            for (StockRow inv : inventories) {
                long stock = inv.totalStock();
                if (stock <= 0 || inv.batch() == null) {
                    continue;
                }
                hasInStock = true;

                LocalDate expiry = inv.batch().getExpiryDate();
                if (expiry == null) {
                    hasHealthy = true;          // no expiry -> healthy
                } else if (expiry.isBefore(today)) {
                    hasExpired = true;
                } else if (!expiry.isAfter(in60Days)) {
                    // near expiry: today .. today + 60
                    if (nearestNearExpiry == null || expiry.isBefore(nearestNearExpiry)) {
                        nearestNearExpiry = expiry;
                    }
                } else {
                    hasHealthy = true;          // > today + 60
                }
            }

            // products with no in-stock batch count only towards the total
            if (!hasInStock) {
                continue;
            }

            if (nearestNearExpiry != null) {
                if (!nearestNearExpiry.isAfter(in30Days)) {
                    expiring0To30++;            // today .. today + 30
                } else {
                    expiring31To60++;          // today + 31 .. today + 60
                }
            } else if (hasHealthy) {
                healthy++;
            } else if (hasExpired) {
                expired++;
            }
        }

        ProductExpiryKpiDto dto = new ProductExpiryKpiDto();
        dto.setTotalProducts(products.size());
        dto.setExpired(expired);
        dto.setExpiring0To30Days(expiring0To30);
        dto.setExpiring31To60Days(expiring31To60);
        dto.setHealthyAbove60Days(healthy);
        return dto;
    }

    // ===== Dashboard KPI: batch counts bucketed independently by each batch's expiry =====
    @Override
    @Transactional(readOnly = true)
    public BatchExpiryKpiDto getBatchExpiryKpi() {

        LocationContext loc = currentLocation();

        // Aggregate in-stock quantity per batch across the location's stock rows,
        // so a batch spread over multiple inventory rows is counted once.
        Map<String, Long> stockByBatch =
                stockRowsForLocation(loc)
                        .stream()
                        .filter(row -> row.batch() != null)
                        .collect(Collectors.groupingBy(
                                row -> row.batch().getBatchId(),
                                Collectors.summingLong(StockRow::totalStock)));

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        LocalDate in60Days = today.plusDays(60);

        long expired = 0;
        long expiring0To30 = 0;
        long expiring31To60 = 0;
        long healthy = 0;

        List<BatchDetails> batches = batchesForLocation(loc);

        // Each in-stock batch is classified independently by its own expiry date
        // (no priority collapsing — that only applies when reducing a product to one status).
        for (BatchDetails batch : batches) {
            long stock = stockByBatch.getOrDefault(batch.getBatchId(), 0L);
            if (stock <= 0) {
                continue; // out-of-stock batches count only towards totalBatches
            }

            LocalDate expiry = batch.getExpiryDate();
            if (expiry == null) {
                healthy++;                     // no expiry -> healthy
            } else if (expiry.isBefore(today)) {
                expired++;
            } else if (!expiry.isAfter(in30Days)) {
                expiring0To30++;               // today .. today + 30
            } else if (!expiry.isAfter(in60Days)) {
                expiring31To60++;              // today + 31 .. today + 60
            } else {
                healthy++;                     // > today + 60
            }
        }

        BatchExpiryKpiDto dto = new BatchExpiryKpiDto();
        dto.setExpiredBatches(expired);
        dto.setExpiring0To30DaysBatches(expiring0To30);
        dto.setExpiring31To60DaysBatches(expiring31To60);
        dto.setHealthyAbove60DaysBatches(healthy);
        // totalBatches counts ALL batches of the location (including out-of-stock)
        dto.setTotalBatches(batches.size());
        dto.setTotalProducts(productsForLocation(loc).size());
        return dto;
    }

    // ===== API 2: complete product details with batches grouped per package =====
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDto getProductDetails(String productId) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        LocationContext loc = currentLocation();

        ProductDetailResponseDto dto = new ProductDetailResponseDto();
        dto.setProductId(product.getProductId());
        // OLD single-pharmacy DTO mapping:
        // if (product.getPharmacy() != null) {
        //     dto.setPharmacyId(product.getPharmacy().getPharmacyId());
        // }
        // Prefer the current pharmacy (validated by checkAuthorization); fall back to the first mapping.
        // For a warehouse manager there is no pharmacy in context, so pharmacyId is left null.
        if (loc.isPharmacy()
                && product.getPharmacies() != null && !product.getPharmacies().isEmpty()) {
            String currentPharmacy = loc.getLocationId();
            String pharmacyId = product.getPharmacies().stream()
                    .map(PharmacyDetails::getPharmacyId)
                    .filter(id -> id.equals(currentPharmacy))
                    .findFirst()
                    .orElse(product.getPharmacies().get(0).getPharmacyId());
            dto.setPharmacyId(pharmacyId);
        }
        if (product.getProductCategory() != null) {
            dto.setProductCategoryId(product.getProductCategory().getProductCategoryId());
        }
        dto.setProductName(product.getProductName());
        dto.setBrandName(product.getBrandName());
        dto.setGstPercentage(product.getGstPercentage());
        dto.setHsnNo(product.getHsnNo());

        // per-batch stock comes from the location's inventory rows, not the batch row
        Map<String, Long> stockByBatch =
                stockRowsForProduct(loc, product.getProductId())
                        .stream()
                        .filter(row -> row.batch() != null)
                        .collect(Collectors.groupingBy(
                                row -> row.batch().getBatchId(),
                                Collectors.summingLong(StockRow::totalStock)));

        List<BatchDetails> batches = product.getBatchDetails() == null
                ? new ArrayList<>()
                : product.getBatchDetails();

        // batches with no package linkage are surfaced separately
        List<BatchDetailsDto> unassigned = batches.stream()
                .filter(b -> b.getPackagingDetails() == null)
                .map(b -> toBatchDto(b, stockByBatch))
                .collect(Collectors.toList());
        dto.setUnassignedBatches(unassigned);

        List<PackageWithBatchesDto> packages = new ArrayList<>();
        if (product.getPackagingDetails() != null) {
            for (PackagingDetails pack : product.getPackagingDetails()) {
                PackageWithBatchesDto packDto = new PackageWithBatchesDto();
                packDto.setPackagingId(pack.getPackagingId());
                packDto.setPurchaseUnit(pack.getPurchaseUnit());
                packDto.setPurchaseUnitContains(pack.getPurchaseUnitContains());
                // packDto.setSmallestUnit(pack.getSmallestUnit());
                if (pack.getPurchaseSmallestUnit() != null) {
                    packDto.setPurchaseSmallestUnitId(pack.getPurchaseSmallestUnit().getPurchaseSmallestUnitId());
                    packDto.setPurchaseSmallestUnitName(pack.getPurchaseSmallestUnit().getPurchaseSmallestUnitName());
                }

                List<BatchDetailsDto> packBatches = batches.stream()
                        .filter(b -> b.getPackagingDetails() != null
                                && pack.getPackagingId().equals(b.getPackagingDetails().getPackagingId()))
                        .map(b -> toBatchDto(b, stockByBatch))
                        .collect(Collectors.toList());
                packDto.setBatches(packBatches);

                packages.add(packDto);
            }
        }
        dto.setPackages(packages);

        return dto;
    }

    // maps a batch and overrides its stockQuantity with the value from pharma_inventory
    private BatchDetailsDto toBatchDto(BatchDetails batch, Map<String, Long> stockByBatch) {
        BatchDetailsDto batchDto = inventoryMapper.toDto(batch);
        batchDto.setStockQuantity(stockByBatch.getOrDefault(batch.getBatchId(), 0L));
        return batchDto;
    }

    // ===== Add a new package (optionally with batches) to an existing product =====
    @Override
    @Transactional
    public ProductDetailResponseDto addPackage(String productId, AddPackageRequest request) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        String pharmacyName = resolvePharmacyName(product);
        String createdBy = resolveCreatedBy();
        LocalDateTime now = LocalDateTime.now();

        PackagingDetails pkg = new PackagingDetails();
        pkg.setPackagingId(generatePackagingId(pharmacyName));
        pkg.setProduct(product);
        // purchaseUnit is derived from the linked PurchaseSmallestUnit master
        // pkg.setPurchaseUnit(request.getPurchaseUnit());
        pkg.setPurchaseUnitContains(request.getPurchaseUnitContains());
        // pkg.setSmallestUnit(request.getSmallestUnit());
        inventoryMapper.applyPurchaseSmallestUnit(pkg, request.getPurchaseSmallestUnitId());
        pkg.setCreatedBy(createdBy);
        pkg.setCreatedAt(now);

        List<BatchDetailsDto> batchDtos = request.getBatches() == null
                ? List.of()
                : request.getBatches();

        List<String> batchIds = nextBatchIds(pharmacyName, batchDtos.size());
        List<BatchDetails> batchEntities = new ArrayList<>();
        for (int i = 0; i < batchDtos.size(); i++) {
            BatchDetails batch = inventoryMapper.toEntity(batchDtos.get(i), createdBy, now);
            batch.setBatchId(batchIds.get(i));
            batch.setProduct(product);
            batch.setPackagingDetails(pkg);
            batchEntities.add(batch);
        }
        pkg.setBatchDetails(batchEntities);

        // cascade ALL on PackagingDetails.batchDetails persists the batches too
        packagingRepo.save(pkg);

        return getProductDetails(productId);
    }

    // ===== Add batches to existing packages of a product =====
    @Override
    @Transactional
    public ProductDetailResponseDto addBatches(String productId, List<BatchDetailsDto> batches) {
        if (batches == null || batches.isEmpty()) {
            throw new RuntimeException("At least one batch is required.");
        }

        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        // packages that belong to this product, keyed by id, for ownership validation
        Map<String, PackagingDetails> ownPackages = product.getPackagingDetails() == null
                ? Map.of()
                : product.getPackagingDetails().stream()
                .collect(Collectors.toMap(PackagingDetails::getPackagingId, p -> p));

        String pharmacyName = resolvePharmacyName(product);
        String createdBy = resolveCreatedBy();
        LocalDateTime now = LocalDateTime.now();

        List<String> batchIds = nextBatchIds(pharmacyName, batches.size());
        List<BatchDetails> batchEntities = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            BatchDetailsDto bDto = batches.get(i);

            String packagingId = bDto.getPackagingId();
            if (packagingId == null || packagingId.isBlank()) {
                throw new RuntimeException("packagingId is required for each batch.");
            }
            PackagingDetails pkg = ownPackages.get(packagingId);
            if (pkg == null) {
                throw new RuntimeException(
                        "Packaging " + packagingId + " does not belong to product " + productId);
            }

            BatchDetails batch = inventoryMapper.toEntity(bDto, createdBy, now);
            batch.setBatchId(batchIds.get(i));
            batch.setProduct(product);
            batch.setPackagingDetails(pkg);
            batchEntities.add(batch);
        }

        batchRepo.saveAll(batchEntities);

        return getProductDetails(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsDto getProductById(String productId) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        return mapper.toDto(product);
    }

    @Override
    @Transactional
    public void deleteProduct(String productId) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        productRepo.deleteById(productId);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getUserId();
        }
        return null;
    }

    // ===== Location (warehouse vs pharmacy) resolution for the read APIs =====
    // A warehouse manager reads warehouse products + pharma_warehouse_inventory;
    // everyone else reads pharmacy products + pharma_inventory. The stock/expiry
    // mapping below is identical for both once stock is normalized to StockRow.

    private UserDetails currentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUser();
        }
        throw new RuntimeException("No authenticated user");
    }

    private LocationContext currentLocation() {
        return locationContextResolver.resolve(currentUserOrThrow());
    }

    /**
     * A batch's stock at a location, decoupled from whether it came from pharmacy or warehouse inventory.
     */
    private record StockRow(ProductDetails product, BatchDetails batch, long totalStock) {

        static StockRow of(Inventory inv) {
            return new StockRow(inv.getProduct(), inv.getBatch(),
                    inv.getTotalStock() == null ? 0L : inv.getTotalStock());
        }

        static StockRow of(WarehouseInventory inv) {
            return new StockRow(inv.getProduct(), inv.getBatch(),
                    inv.getTotalStock() == null ? 0L : inv.getTotalStock());
        }
    }

    private List<ProductDetails> productsForLocation(LocationContext loc) {
        return loc.isWarehouse()
                ? productRepo.findByWarehouses_WarehouseId(loc.getLocationId())
                : productRepo.findByPharmacies_PharmacyId(loc.getLocationId());
    }

    private List<BatchDetails> batchesForLocation(LocationContext loc) {
        return loc.isWarehouse()
                ? batchRepo.findByProduct_Warehouses_WarehouseId(loc.getLocationId())
                : batchRepo.findByProduct_Pharmacies_PharmacyId(loc.getLocationId());
    }

    /**
     * All in-location stock rows, normalized.
     */
    private List<StockRow> stockRowsForLocation(LocationContext loc) {
        if (loc.isWarehouse()) {
            return warehouseInventoryRepo.findByWarehouse_WarehouseId(loc.getLocationId())
                    .stream().map(StockRow::of).collect(Collectors.toList());
        }
        return inventoryRepo.findByPharmacy_PharmacyId(loc.getLocationId())
                .stream().map(StockRow::of).collect(Collectors.toList());
    }

    /**
     * Total stock for one batch at the location.
     */
    private long stockForBatch(LocationContext loc, String batchId) {
        if (loc.isWarehouse()) {
            return warehouseInventoryRepo
                    .findByWarehouse_WarehouseIdAndBatch_BatchId(loc.getLocationId(), batchId)
                    .stream().mapToLong(i -> i.getTotalStock() == null ? 0L : i.getTotalStock()).sum();
        }
        return inventoryRepo
                .findByPharmacy_PharmacyIdAndBatch_BatchId(loc.getLocationId(), batchId)
                .stream().mapToLong(i -> i.getTotalStock() == null ? 0L : i.getTotalStock()).sum();
    }

    /**
     * Normalized stock rows for one product at the location.
     */
    private List<StockRow> stockRowsForProduct(LocationContext loc, String productId) {
        if (loc.isWarehouse()) {
            return warehouseInventoryRepo
                    .findByWarehouse_WarehouseIdAndProduct_ProductId(loc.getLocationId(), productId)
                    .stream().map(StockRow::of).collect(Collectors.toList());
        }
        // Pharmacy path preserved as-is: stock summed across the product's rows.
        return inventoryRepo.findByProduct_ProductId(productId)
                .stream().map(StockRow::of).collect(Collectors.toList());
    }

    /**
     * The batch, scoped to the location's products.
     */
    private BatchDetails batchForLocation(LocationContext loc, String batchId) {
        if (loc.isWarehouse()) {
            return batchRepo
                    .findByBatchIdAndProduct_Warehouses_WarehouseId(batchId, loc.getLocationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Batch not found in this warehouse with id : " + batchId));
        }
        return batchRepo
                .findByBatchIdAndProduct_Pharmacies_PharmacyId(batchId, loc.getLocationId())
                .orElseThrow(() -> new RuntimeException(
                        "Batch not found in this pharmacy with id : " + batchId));
    }

//    private void checkAuthorization(ProductDetails product) {
//        Long userId = getCurrentUserId();
//        if (userId != null && product.getPharmacy() != null) {
//            boolean valid = pharmacyRepo.existsUserPharmacy(product.getPharmacy().getPharmacyId(), userId);
//            if (!valid) {
//                throw new RuntimeException("You are not authorized to access this product.");
//            }
//        }
//    }

    private void checkAuthorization(ProductDetails product) {

        LocationContext loc = currentLocation();

        // Warehouse manager: the product must be mapped to their warehouse.
        if (loc.isWarehouse()) {
            boolean authorized = product.getWarehouses() != null
                    && product.getWarehouses().stream()
                    .anyMatch(w -> w.getWarehouseId().equals(loc.getLocationId()));
            if (!authorized) {
                throw new RuntimeException(
                        "You are not authorized to access this product.");
            }
            return;
        }

        String currentPharmacy = loc.getLocationId();

        // OLD single-pharmacy authorization:
        // if (product.getPharmacy() == null) {
        //     throw new RuntimeException("Product is not mapped to any pharmacy.");
        // }
        // if (!product.getPharmacy().getPharmacyId().equals(currentPharmacy)) {
        //     throw new RuntimeException("You are not authorized to access this product.");
        // }
        if (product.getPharmacies() == null || product.getPharmacies().isEmpty()) {
            throw new RuntimeException("Product is not mapped to any pharmacy.");
        }

        boolean authorized = product.getPharmacies().stream()
                .anyMatch(p -> p.getPharmacyId().equals(currentPharmacy));
        if (!authorized) {
            throw new RuntimeException(
                    "You are not authorized to access this product.");
        }
    }

    private synchronized String generateProductId(String productName, String pharmacyName) {
        String cleanedPharmacy = pharmacyName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        String prefix = cleanedPharmacy.length() >= 2 ? cleanedPharmacy.substring(0, 2) : String.format("%-2s", cleanedPharmacy).replace(' ', 'X');

        String namePart = productName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        namePart = namePart.length() >= 3 ? namePart.substring(0, 3) : String.format("%-3s", namePart).replace(' ', 'X');

        Integer lastNumber = productRepo.findMaxProductNumber();
        int nextNumber = (lastNumber == null) ? 1 : lastNumber + 1;
        return prefix + namePart + String.format("%05d", nextNumber);
    }

    // Resolves the name of the location the product belongs to (pharmacy or warehouse),
    // used only as a prefix for generated package/batch ids.
    private String resolvePharmacyName(ProductDetails product) {

        // OLD (pharmacy-only) — replaced by location-aware resolution below:
        // String pharmacyId = pharmacyContext.getCurrentPharmacy();
        // String name = (product.getPharmacies() == null)
        //         ? null
        //         : product.getPharmacies().stream()
        //         .filter(p -> p.getPharmacyId().equals(pharmacyId))
        //         .map(PharmacyDetails::getPharmacyName)
        //         .findFirst()
        //         .orElse(null);
        // return name == null ? "XX" : name;

        LocationContext loc = currentLocation();

        if (loc.isWarehouse()) {
            String name = (product.getWarehouses() == null)
                    ? null
                    : product.getWarehouses().stream()
                    .filter(w -> w.getWarehouseId().equals(loc.getLocationId()))
                    .map(Warehouse::getWarehouseName)
                    .findFirst()
                    .orElse(null);
            return name == null ? "XX" : name;
        }

        String pharmacyId = loc.getLocationId();
        String name = (product.getPharmacies() == null)
                ? null
                : product.getPharmacies().stream()
                .filter(p -> p.getPharmacyId().equals(pharmacyId))
                .map(PharmacyDetails::getPharmacyName)
                .findFirst()
                .orElse(null);
        return name == null ? "XX" : name;
    }

    private String resolveCreatedBy() {
        String userId = getCurrentUserId();
        return userId != null ? userId : "System";
    }

    private String pharmacyPrefix(String pharmacyName) {
        String cleaned = pharmacyName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        return cleaned.length() >= 2
                ? cleaned.substring(0, 2)
                : String.format("%-2s", cleaned).replace(' ', 'X');
    }

    // Generates a contiguous block of batch ids in one shot so multiple batches
    // created in a single request don't collide (findMaxBatchNumber only sees
    // persisted rows). Synchronized to match the single-id generators.
    private synchronized List<String> nextBatchIds(String pharmacyName, int count) {
        List<String> ids = new ArrayList<>();
        if (count <= 0) {
            return ids;
        }
        String prefix = pharmacyPrefix(pharmacyName);
        Integer lastNumber = batchRepo.findMaxBatchNumber();
        int next = (lastNumber == null) ? 0 : lastNumber;
        for (int i = 0; i < count; i++) {
            ids.add(prefix + "BTCH" + String.format("%05d", ++next));
        }
        return ids;
    }

    private synchronized String generatePackagingId(String pharmacyName) {
        String cleanedPharmacy = pharmacyName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        String prefix = cleanedPharmacy.length() >= 2 ? cleanedPharmacy.substring(0, 2) : String.format("%-2s", cleanedPharmacy).replace(' ', 'X');

        Integer lastNumber = packagingRepo.findMaxPackagingNumber();
        int nextNumber = (lastNumber == null) ? 1 : lastNumber + 1;
        return prefix + "PKG" + String.format("%05d", nextNumber);
    }

    private synchronized String generateBatchId(String pharmacyName) {
        String cleanedPharmacy = pharmacyName.replaceAll("[^a-zA-Z]", "").toUpperCase();
        String prefix = cleanedPharmacy.length() >= 2 ? cleanedPharmacy.substring(0, 2) : String.format("%-2s", cleanedPharmacy).replace(' ', 'X');

        Integer lastNumber = batchRepo.findMaxBatchNumber();
        int nextNumber = (lastNumber == null) ? 1 : lastNumber + 1;
        return prefix + "BTCH" + String.format("%05d", nextNumber);
    }

    // ===== Batch listing: batch + product + packaging + available stock =====
    @Override
    @Transactional(readOnly = true)
    public List<BatchStockDto> getAllBatches() {
        return getBatches(currentLocation());
    }

    // Batches at an arbitrary pharmacy rather than the caller's currently active one —
    // needed when picking products for a pharmacy-to-pharmacy transfer whose source
    // pharmacy (chosen in the transfer wizard) differs from the pharmacy the caller is
    // scoped to via X-Pharmacy-Id. Without this, the product list came back for the
    // wrong pharmacy and dispatch failed with "No pharmacy stock for this batch".
    @Override
    @Transactional(readOnly = true)
    public List<BatchStockDto> getBatchesForPharmacy(String pharmacyId) {
        PharmacyDetails pharmacy = pharmacyRepo.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found: " + pharmacyId));

        Long callerOrgId = organizationService
                .getUserOrganization(currentUserOrThrow().getUserId())
                .getOrganizationId();
        Long pharmacyOrgId = pharmacy.getOrganization() == null
                ? null : pharmacy.getOrganization().getOrganizationId();
        if (!callerOrgId.equals(pharmacyOrgId)) {
            throw new IllegalArgumentException("Pharmacy does not belong to your organization");
        }

        return getBatches(new LocationContext(LocationType.PHARMACY, pharmacyId));
    }

    private List<BatchStockDto> getBatches(LocationContext loc) {

        // stock lives in inventory rows, not on the batch -> load the location's
        // stock rows once and total them per batch.
        Map<String, Long> stockByBatch =
                stockRowsForLocation(loc)
                        .stream()
                        .filter(row -> row.batch() != null)
                        .collect(Collectors.groupingBy(
                                row -> row.batch().getBatchId(),
                                Collectors.summingLong(StockRow::totalStock)));

        return batchesForLocation(loc)
                .stream()
                .map(batch -> toBatchStock(
                        batch,
                        stockByBatch.getOrDefault(batch.getBatchId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchStockDto getBatchById(String batchId) {

        LocationContext loc = currentLocation();

        BatchDetails batch = batchForLocation(loc, batchId);

        long totalStock = stockForBatch(loc, batchId);

        return toBatchStock(batch, totalStock);
    }

    private BatchStockDto toBatchStock(BatchDetails batch, long totalStock) {

        BatchStockDto dto = new BatchStockDto();

        dto.setBatchId(batch.getBatchId());
        dto.setBatchNumber(batch.getBatchNumber());
        dto.setManufacturingDate(batch.getManufacturingDate());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setRackLocation(batch.getRackLocation());

        ProductDetails product = batch.getProduct();

        if (product != null) {
            dto.setProductId(product.getProductId());
            dto.setProductName(product.getProductName());
            dto.setBrandName(product.getBrandName());
            dto.setGstPercentage(product.getGstPercentage());
            dto.setHsnNo(product.getHsnNo());
        }

        PackagingDetails packaging = batch.getPackagingDetails();

        if (packaging != null) {
            dto.setPackagingId(packaging.getPackagingId());
            dto.setPurchaseUnit(packaging.getPurchaseUnit());
            dto.setPurchaseUnitContains(packaging.getPurchaseUnitContains());

            if (packaging.getPurchaseSmallestUnit() != null) {
                dto.setPurchaseSmallestUnitId(
                        packaging.getPurchaseSmallestUnit().getPurchaseSmallestUnitId());
                dto.setPurchaseSmallestUnitName(
                        packaging.getPurchaseSmallestUnit().getPurchaseSmallestUnitName());
            }
        }

        dto.setTotalStock(totalStock);

        dto.setPurchasePrice(batch.getPurchasePrice());
        dto.setMrp(batch.getMrp());
        dto.setSellingPrice(batch.getSellingPrice());
        dto.setPurchasePricePerUnit(batch.getPurchasePricePerUnit());
        dto.setMrpPerUnit(batch.getMrpPerUnit());
        dto.setSellingPricePerUnit(batch.getSellingPricePerUnit());

        dto.setStatus(resolveBatchStatus(batch.getExpiryDate(), totalStock));

        return dto;
    }

    private String resolveBatchStatus(LocalDate expiryDate, long totalStock) {

        if (totalStock <= 0L) {
            return "OUT_OF_STOCK";
        }

        if (expiryDate == null) {
            return "ACTIVE";
        }

        LocalDate today = LocalDate.now();

        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }

        if (!expiryDate.isAfter(today.plusDays(NEAR_EXPIRY_DAYS))) {
            return "NEAR_EXPIRY";
        }

        return "ACTIVE";
    }


    @Override
    public boolean existsByBatchNumber(
            UserDetails user,
            String batchNumber,
            String productId,
            String packagingId) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext loc = locationContextResolver.resolve(persistentUser);

        // Pharmacy users must belong to the selected pharmacy; a warehouse manager is
        // already scoped to their own warehouse, so no pharmacy check applies.
        if (loc.isPharmacy()) {
            boolean valid = pharmacyRepo.existsUserPharmacy(
                    loc.getLocationId(),
                    persistentUser.getUserId()
            );

            if (!valid) {
                throw new RuntimeException(
                        "You are not authorized to use this pharmacy."
                );
            }
        }

        return batchRepo
                .existsByBatchNumberAndProduct_ProductIdAndPackagingDetails_PackagingId(
                        batchNumber,
                        productId,
                        packagingId
                );
    }
}
