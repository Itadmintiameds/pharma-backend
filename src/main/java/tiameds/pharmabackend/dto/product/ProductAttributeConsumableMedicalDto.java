package tiameds.pharmabackend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class ProductAttributeConsumableMedicalDto {
    private String productAttributeId;
    
    private Long deviceCategoryId;
    private Long deviceSubCategoryId;
    
    private List<Long> materialTypeIds;
    
    private String dimensionSize;
    private String sterileOrNonSterile;
    private String disposalOrNonDisposal;
    private String purpose;
    private String manufacturerName;
    private String manufacturerLicenseNumber;
    private Boolean isISOCertified;
}
