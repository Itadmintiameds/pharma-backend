package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class PurchaseSmallestUnitDto {
    private Long purchaseSmallestUnitId;
    private String purchaseSmallestUnitName;
    private String purchaseUnitName;
    private Long productCategoryId;
    private String productCategoryName;
    private Boolean isActive;
}
