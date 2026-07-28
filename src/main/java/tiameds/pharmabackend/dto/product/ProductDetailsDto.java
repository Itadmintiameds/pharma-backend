package tiameds.pharmabackend.dto.product;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductDetailsDto {
    private String productId;
    private String pharmacyId;
    private Long productCategoryId;
    private String productName;
    private String brandName;
    private BigDecimal gstPercentage;
    private String hsnNo;
    
    private List<ProductAttributeDrugDto> productAttributeDrugs;
    private List<ProductAttributeCosmeticsDto> productAttributeCosmetics;
    private List<ProductAttributeSupplementsDto> productAttributeSupplements;
    private List<BatchDetailsDto> batchDetails;
    private List<PackagingDetailsDto> packagingDetails;
}
