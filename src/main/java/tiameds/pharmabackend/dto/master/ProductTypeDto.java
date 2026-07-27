package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class ProductTypeDto {
    private Long productTypeId;
    private String productTypeName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
