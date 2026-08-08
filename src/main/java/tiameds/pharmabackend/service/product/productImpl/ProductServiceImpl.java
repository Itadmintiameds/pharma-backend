package tiameds.pharmabackend.service.product.productImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.product.*;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.mapper.product.ProductMapper;
import tiameds.pharmabackend.mapper.product.category.ProductInventoryMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.security.CustomUserDetails;
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
    private UserDetailsRepository userDetailsRepository;

    // A batch is "near expiry" when it expires within this many days from today.
    private static final long NEAR_EXPIRY_DAYS = 30;

    @Override
    @Transactional
    public ProductDetailsDto onboardProduct(ProductDetailsDto dto) {

//        PharmacyDetails pharmacy = pharmacyRepo.findById(dto.getPharmacyId())
//                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        PharmacyDetails pharmacy = pharmacyRepo.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        String createdBy = "System";
        String userId = getCurrentUserId();
        if (userId != null) {
            createdBy = userId;
        }

//        if (userId != null) {
//            boolean valid = pharmacyRepo.existsUserPharmacy(dto.getPharmacyId(), userId);
//            if (!valid) {
//                throw new RuntimeException("You are not authorized to use this pharmacy.");
//            }
//        }

        String rawPharmacyName = pharmacy.getPharmacyName();
        final String pharmacyName = (rawPharmacyName == null) ? "XX" : rawPharmacyName;

        String productId = generateProductId(dto.getProductName(), pharmacyName);

        ProductDetails product = mapper.toEntity(dto, productId, createdBy, LocalDateTime.now());

        // OLD single-pharmacy assignment: product.setPharmacy(pharmacy);
        // Product now maps to many pharmacies via ManyToMany.
        if (product.getPharmacies() == null) {
            product.setPharmacies(new ArrayList<>());
        }
        product.getPharmacies().add(pharmacy);
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

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        return productRepo.findByPharmacies_PharmacyId(pharmacyId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // ===== API 1: products of the current pharmacy with stock + expiry status =====
    @Override
    @Transactional(readOnly = true)
    public List<ProductStockSummaryDto> getProductStockSummaries() {

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        // stock lives in pharma_inventory, not on the batch -> load all stock
        // rows for the pharmacy once and group them by product.
        Map<String, List<Inventory>> inventoryByProduct =
                inventoryRepo.findByPharmacy_PharmacyId(pharmacyId)
                        .stream()
                        .filter(inv -> inv.getProduct() != null)
                        .collect(Collectors.groupingBy(inv -> inv.getProduct().getProductId()));

        return productRepo.findByPharmacies_PharmacyId(pharmacyId)
                .stream()
                .map(product -> toStockSummary(
                        product,
                        inventoryByProduct.getOrDefault(product.getProductId(), List.of())))
                .collect(Collectors.toList());
    }

    private ProductStockSummaryDto toStockSummary(ProductDetails product, List<Inventory> inventories) {
        ProductStockSummaryDto dto = new ProductStockSummaryDto();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());

        LocalDate today = LocalDate.now();
        LocalDate nearExpiryCutoff = today.plusDays(NEAR_EXPIRY_DAYS);

        long totalStock = 0L;
        long active = 0L;
        long nearExpiry = 0L;
        long expired = 0L;
        LocalDate nearestExpiry = null;

        for (Inventory inv : inventories) {
            long stock = inv.getTotalStock() == null ? 0L : inv.getTotalStock();

            totalStock += stock;

            // status is counted only over stock rows that actually hold stock
            if (stock <= 0 || inv.getBatch() == null) {
                continue;
            }

            LocalDate expiry = inv.getBatch().getExpiryDate();
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

    private String resolveOverallStatus(long active, long nearExpiry, long expired) {
        if (expired > 0) {
            return "EXPIRED";
        }
        if (nearExpiry > 0) {
            return "NEAR_EXPIRY";
        }
        if (active > 0) {
            return "ACTIVE";
        }
        return "OUT_OF_STOCK";
    }

    // ===== Dashboard KPI: product counts bucketed by nearest in-stock expiry =====
    @Override
    @Transactional(readOnly = true)
    public ProductExpiryKpiDto getExpiryKpi() {

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        // in-stock rows for the pharmacy, grouped by product
        Map<String, List<Inventory>> inventoryByProduct =
                inventoryRepo.findByPharmacy_PharmacyId(pharmacyId)
                        .stream()
                        .filter(inv -> inv.getProduct() != null)
                        .collect(Collectors.groupingBy(inv -> inv.getProduct().getProductId()));

        List<ProductDetails> products = productRepo.findByPharmacies_PharmacyId(pharmacyId);

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        LocalDate in60Days = today.plusDays(60);

        long expired = 0;
        long expiring0To30 = 0;
        long expiring31To60 = 0;
        long healthy = 0;

        for (ProductDetails product : products) {
            List<Inventory> inventories =
                    inventoryByProduct.getOrDefault(product.getProductId(), List.of());

            LocalDate nearest = null;
            boolean hasInStock = false;

            for (Inventory inv : inventories) {
                long stock = inv.getTotalStock() == null ? 0L : inv.getTotalStock();
                if (stock <= 0 || inv.getBatch() == null) {
                    continue;
                }
                hasInStock = true;

                LocalDate expiry = inv.getBatch().getExpiryDate();
                if (expiry == null) {
                    continue; // no expiry -> does not affect nearest
                }
                if (nearest == null || expiry.isBefore(nearest)) {
                    nearest = expiry;
                }
            }

            // products with no in-stock batch count only towards the total
            if (!hasInStock) {
                continue;
            }

            // only null-expiry stock on hand -> treat as healthy
            if (nearest == null) {
                healthy++;
            } else if (nearest.isBefore(today)) {
                expired++;
            } else if (!nearest.isAfter(in30Days)) {
                expiring0To30++;               // today .. today + 30
            } else if (!nearest.isAfter(in60Days)) {
                expiring31To60++;              // today + 31 .. today + 60
            } else {
                healthy++;                     // > today + 60
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

    // ===== API 2: complete product details with batches grouped per package =====
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDto getProductDetails(String productId) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        ProductDetailResponseDto dto = new ProductDetailResponseDto();
        dto.setProductId(product.getProductId());
        // OLD single-pharmacy DTO mapping:
        // if (product.getPharmacy() != null) {
        //     dto.setPharmacyId(product.getPharmacy().getPharmacyId());
        // }
        // Prefer the current pharmacy (validated by checkAuthorization); fall back to the first mapping.
        if (product.getPharmacies() != null && !product.getPharmacies().isEmpty()) {
            String currentPharmacy = pharmacyContext.getCurrentPharmacy();
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

        // per-batch stock comes from pharma_inventory, not the batch row
        Map<String, Long> stockByBatch =
                inventoryRepo.findByProduct_ProductId(product.getProductId())
                        .stream()
                        .filter(inv -> inv.getBatch() != null)
                        .collect(Collectors.groupingBy(
                                inv -> inv.getBatch().getBatchId(),
                                Collectors.summingLong(inv ->
                                        inv.getTotalStock() == null ? 0L : inv.getTotalStock())));

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

        String currentPharmacy =
                pharmacyContext.getCurrentPharmacy();

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

    private String resolvePharmacyName(ProductDetails product) {
        // OLD: product.getPharmacy() == null ? null : product.getPharmacy().getPharmacyName();
        String pharmacyId = pharmacyContext.getCurrentPharmacy();
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

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        // stock lives in pharma_inventory, not on the batch -> load the pharmacy's
        // stock rows once and total them per batch.
        Map<String, Long> stockByBatch =
                inventoryRepo.findByPharmacy_PharmacyId(pharmacyId)
                        .stream()
                        .filter(inv -> inv.getBatch() != null)
                        .collect(Collectors.groupingBy(
                                inv -> inv.getBatch().getBatchId(),
                                Collectors.summingLong(inv ->
                                        inv.getTotalStock() == null ? 0L : inv.getTotalStock())));

        return batchRepo.findByProduct_Pharmacies_PharmacyId(pharmacyId)
                .stream()
                .map(batch -> toBatchStock(
                        batch,
                        stockByBatch.getOrDefault(batch.getBatchId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchStockDto getBatchById(String batchId) {

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        BatchDetails batch = batchRepo
                .findByBatchIdAndProduct_Pharmacies_PharmacyId(batchId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Batch not found in this pharmacy with id : " + batchId));

        long totalStock = inventoryRepo
                .findByPharmacy_PharmacyIdAndBatch_BatchId(pharmacyId, batchId)
                .stream()
                .mapToLong(inv -> inv.getTotalStock() == null ? 0L : inv.getTotalStock())
                .sum();

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

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyRepo.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId()
        );

        if (!valid) {
            throw new RuntimeException(
                    "You are not authorized to use this pharmacy."
            );
        }

        return batchRepo
                .existsByBatchNumberAndProduct_ProductIdAndPackagingDetails_PackagingId(
                        batchNumber,
                        productId,
                        packagingId
                );
    }
}
