package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PharmaProductDetailsDto {
    private String productId;
    private String pharmacyId;
    private Long productCategoryId;
    private String productName;
    private String brandName;
    private BigDecimal gstPercentage;
    private String hsnNo;
    private String createdBy;
    private LocalDateTime createdAt;
    
    private List<PharmaProductAttributeDrugDto> productAttributeDrugs;
    private List<PharmaProductAttributeSupplementsDto> productAttributeSupplements;
    private List<PharmaBatchDetailsDto> batchDetails;
    private List<PharmaPackagingDetailsDto> packagingDetails;
}
