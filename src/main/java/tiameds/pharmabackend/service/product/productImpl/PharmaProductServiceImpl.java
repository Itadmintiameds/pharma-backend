package tiameds.pharmabackend.service.product.productImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.product.PharmaProductDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.product.PharmaProductDetails;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.product.PharmaBatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PharmaPackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.PharmaProductDetailsRepository;
import tiameds.pharmabackend.service.product.PharmaProductService;
import tiameds.pharmabackend.mapper.product.PharmaProductMapper;

@Service
public class PharmaProductServiceImpl implements PharmaProductService {

    @Autowired
    private PharmaProductDetailsRepository productRepo;
    
    @Autowired
    private PharmaPackagingDetailsRepository packagingRepo;
    
    @Autowired
    private PharmaBatchDetailsRepository batchRepo;
    
    @Autowired
    private PharmacyDetailsRepository pharmacyRepo;
    
    @Autowired
    private PharmaProductMapper mapper;

    @Override
    @Transactional
    public String onboardProduct(PharmaProductDetailsDto dto) {
        
        PharmacyDetails pharmacy = pharmacyRepo.findById(dto.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
                
        String rawPharmacyName = pharmacy.getPharmacyName();
        final String pharmacyName = (rawPharmacyName == null) ? "XX" : rawPharmacyName;

        String productId = generateProductId(dto.getProductName(), pharmacyName);
        
        PharmaProductDetails product = mapper.toEntity(dto, productId);
        
        // Generate IDs and set bidirectional relationships
        if (product.getBatchDetails() != null) {
            product.getBatchDetails().forEach(batch -> {
                batch.setBatchId(generateBatchId(pharmacyName));
                batch.setProduct(product);
            });
        }
        
        if (product.getPackagingDetails() != null) {
            product.getPackagingDetails().forEach(pkg -> {
                pkg.setPackagingId(generatePackagingId(pharmacyName));
                pkg.setProduct(product);
            });
        }
        
        // Setup supplements relationship
        if (product.getProductAttributeSupplements() != null) {
            product.getProductAttributeSupplements().forEach(supp -> {
                supp.setProductAttributeId(productId + "_SUPP");
                supp.setProduct(product);
            });
        }
        
        productRepo.save(product);
        return productId;
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
