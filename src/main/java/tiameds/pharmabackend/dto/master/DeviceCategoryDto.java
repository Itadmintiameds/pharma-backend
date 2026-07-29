package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class DeviceCategoryDto {
    private Long deviceCategoryId;
    private String deviceCategoryName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
