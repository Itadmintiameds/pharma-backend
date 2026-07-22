package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PharmaProductAttributeSupplementsDto {
    private String productAttributeId;
    private Long therapeuticCategoryId;
    private Long therapeuticSubcategoryId;
    private Long flavourId;
    private Long dosageFormId;
    private Long ageGroupId;
    
    private String strengthComposition;
    private Double netQuantity;
    private String netQuantityUnit;
    private String gender;
    private String manufacturerName;
    private String fssaiLicenseNumber;
}
