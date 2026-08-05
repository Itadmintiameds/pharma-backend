package tiameds.pharmabackend.dto.billing;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingDetailsDto {

    private Long billingDetailsId;
    private Long billingId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private String unit;
    private Long billQuantity;
    private BigDecimal grossAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal gstAmount;
    private BigDecimal netAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
