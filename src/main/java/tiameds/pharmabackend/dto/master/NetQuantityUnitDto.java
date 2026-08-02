package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class NetQuantityUnitDto {
    private Long netQuantityUnitId;
    private String netQuantityUnitName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
