package tiameds.pharmabackend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class ProductAttributeFoodInfantDto {
    private String productAttributeId;
    
    private Long productTypeId;
    private Long productSubTypeId;
    private Long productFormId;
    
    private String variantName;
    
    private List<Long> ageGroupIds;
    
    private Double netQuantity;
    private Long netQuantityUnitId;
    private String manufacturerName;
}