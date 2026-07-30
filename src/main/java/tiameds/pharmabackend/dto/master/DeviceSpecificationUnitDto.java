package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class DeviceSpecificationUnitDto {
    private Long deviceSpecificationUnitId;
    private String deviceSpecificationUnitName;
    private Long deviceSubCategoryId;
    private String deviceSubCategoryName;
    private Boolean isActive;
}
