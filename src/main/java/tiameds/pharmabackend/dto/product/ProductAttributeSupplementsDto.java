package tiameds.pharmabackend.dto.product;

import lombok.Data;

@Data
public class ProductAttributeSupplementsDto {
    private String productAttributeId;
    private Long therapeuticCategoryId;
    private Long therapeuticSubcategoryId;
    private Long flavourId;
    private Long dosageFormId;
    private Long ageGroupId;
    
    private String strengthComposition;
    private Double netQuantity;
    private Long netQuantityUnitId;
    private String gender;
    private String manufacturerName;
    private String fssaiLicenseNumber;
}
