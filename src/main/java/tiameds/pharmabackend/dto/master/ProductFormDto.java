package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class ProductFormDto {
    private Long productFormId;
    private String productFormName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
