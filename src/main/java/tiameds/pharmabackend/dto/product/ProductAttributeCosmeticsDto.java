package tiameds.pharmabackend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class ProductAttributeCosmeticsDto {
    private String productAttributeId;
    
    private Long productTypeId;
    private Long productSubTypeId;
    private Long productFormId;
    
    private String variantName;
    
    private List<Long> intendedUseAreaIds;
    private List<Long> skinTypeIds;
    private List<Long> hairTypeIds;
    private List<Long> ageGroupIds;
    
    private String gender;
    private String fragrance;
    
    private Double netQuantity;
    private Long netQuantityUnitId;
    private String manufacturerName;
}