package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class ProductCategoryDto {
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
