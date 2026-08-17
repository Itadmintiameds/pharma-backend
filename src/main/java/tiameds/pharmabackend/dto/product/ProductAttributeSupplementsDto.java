package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.util.List;

@Data
public class ProductAttributeSupplementsDto {
    private String productAttributeId;
    private Long therapeuticCategoryId;
    private Long therapeuticSubcategoryId;
    private Long flavourId;
    private Long dosageFormId;
    private List<Long> ageGroupIds;
    
    private String strengthComposition;
    private Double netQuantity;
    private Long netQuantityUnitId;
    private String gender;
    private String manufacturerName;
    private String fssaiLicenseNumber;
}
