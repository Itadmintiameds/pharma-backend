package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class MaterialTypeDto {
    private Long materialTypeId;
    private String materialTypeName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
