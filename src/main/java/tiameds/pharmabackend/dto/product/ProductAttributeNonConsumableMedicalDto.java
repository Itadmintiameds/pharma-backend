package tiameds.pharmabackend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class ProductAttributeNonConsumableMedicalDto {
    private String productAttributeId;
    
    private Long deviceCategoryId;
    private Long deviceSubCategoryId;
    
    private String modelName;
    private String deviceClassification;
    private String purpose;
    private String dimensionSize;
    private Long deviceSpecificationUnitId;
    private List<Long> materialTypeIds;
    
    private Long powerSourceId;
    private String warrantyPeriod;
    private Boolean serviceAvailability;
    private String manufacturerName;
    private Long countryId;
}
