package tiameds.pharmabackend.service.product.productImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.PackageWithBatchesDto;
import tiameds.pharmabackend.dto.product.ProductDetailResponseDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.ProductStockSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.service.product.ProductService;
import tiameds.pharmabackend.mapper.product.ProductMapper;
import tiameds.pharmabackend.mapper.product.category.ProductInventoryMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tiameds.pharmabackend.security.CustomUserDetails;
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
        Long userId = getCurrentUserId();
        if (userId != null) {
            createdBy = String.valueOf(userId);
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

        product.setPharmacy(pharmacy);
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
        return dto;
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

        return productRepo.findAll()
                .stream()
                .filter(product ->
                        product.getPharmacy() != null &&
                                pharmacyId.equals(product.getPharmacy().getPharmacyId()))
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

        return productRepo.findAll()
                .stream()
                .filter(product ->
                        product.getPharmacy() != null &&
                                pharmacyId.equals(product.getPharmacy().getPharmacyId()))
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

    // ===== API 2: complete product details with batches grouped per package =====
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDto getProductDetails(String productId) {
        ProductDetails product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        checkAuthorization(product);

        ProductDetailResponseDto dto = new ProductDetailResponseDto();
        dto.setProductId(product.getProductId());
        if (product.getPharmacy() != null) {
            dto.setPharmacyId(product.getPharmacy().getPharmacyId());
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
                packDto.setSmallestUnit(pack.getSmallestUnit());

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

    private Long getCurrentUserId() {
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

        if (product.getPharmacy() == null) {
            throw new RuntimeException("Product is not mapped to any pharmacy.");
        }

        if (!product.getPharmacy().getPharmacyId().equals(currentPharmacy)) {
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
}
