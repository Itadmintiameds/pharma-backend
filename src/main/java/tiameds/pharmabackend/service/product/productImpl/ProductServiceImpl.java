package tiameds.pharmabackend.service.product.productImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.service.product.ProductService;
import tiameds.pharmabackend.mapper.product.ProductMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tiameds.pharmabackend.security.CustomUserDetails;
import java.time.LocalDateTime;

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

    @Override
    @Transactional
    public ProductDetailsDto onboardProduct(ProductDetailsDto dto) {
        
        PharmacyDetails pharmacy = pharmacyRepo.findById(dto.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        String createdBy = "System";
        Long userId = getCurrentUserId();
        if (userId != null) {
            createdBy = String.valueOf(userId);
        }

        if (userId != null) {
            boolean valid = pharmacyRepo.existsUserPharmacy(dto.getPharmacyId(), userId);
            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }
        }
                
        String rawPharmacyName = pharmacy.getPharmacyName();
        final String pharmacyName = (rawPharmacyName == null) ? "XX" : rawPharmacyName;

        String productId = generateProductId(dto.getProductName(), pharmacyName);
        
        ProductDetails product = mapper.toEntity(dto, productId, createdBy, LocalDateTime.now());
        
        dto.setProductId(productId);

        // Generate IDs and set bidirectional relationships
        if (product.getBatchDetails() != null && dto.getBatchDetails() != null) {
            for (int i = 0; i < product.getBatchDetails().size(); i++) {
                String bId = generateBatchId(pharmacyName);
                product.getBatchDetails().get(i).setBatchId(bId);
                product.getBatchDetails().get(i).setProduct(product);
                dto.getBatchDetails().get(i).setBatchId(bId);
            }
        }
        
        if (product.getPackagingDetails() != null && dto.getPackagingDetails() != null) {
            for (int i = 0; i < product.getPackagingDetails().size(); i++) {
                String pId = generatePackagingId(pharmacyName);
                product.getPackagingDetails().get(i).setPackagingId(pId);
                product.getPackagingDetails().get(i).setProduct(product);
                dto.getPackagingDetails().get(i).setPackagingId(pId);
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
    @Override
    @Transactional(readOnly = true)
    public java.util.List<ProductDetailsDto> getAllProducts() {
        Long userId = getCurrentUserId();
        
        if (userId == null) {
            return productRepo.findAll().stream()
                    .map(mapper::toDto)
                    .collect(java.util.stream.Collectors.toList());
        }

        java.util.List<String> allowedPharmacies = pharmacyRepo.findPharmacyIdsByUserId(userId);
        
        return productRepo.findAll().stream()
                .filter(p -> p.getPharmacy() != null && allowedPharmacies.contains(p.getPharmacy().getPharmacyId()))
                .map(mapper::toDto)
                .collect(java.util.stream.Collectors.toList());
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

    private void checkAuthorization(ProductDetails product) {
        Long userId = getCurrentUserId();
        if (userId != null && product.getPharmacy() != null) {
            boolean valid = pharmacyRepo.existsUserPharmacy(product.getPharmacy().getPharmacyId(), userId);
            if (!valid) {
                throw new RuntimeException("You are not authorized to access this product.");
            }
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
