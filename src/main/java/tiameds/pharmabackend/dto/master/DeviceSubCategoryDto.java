package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class DeviceSubCategoryDto {
    private Long deviceSubCategoryId;
    private String deviceSubCategoryName;
    private Long deviceCategoryId;
    private String deviceCategoryName;
    private Boolean isActive;
}
