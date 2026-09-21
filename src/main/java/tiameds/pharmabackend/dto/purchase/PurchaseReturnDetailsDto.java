package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseReturnDetailsDto {

    private Long purchaseReturnDetailId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private Long purchaseReturnQuantity;
    private String freeReturnQuantity;
    private BigDecimal grossAmount;
    private BigDecimal gstAmount;
    private BigDecimal netAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
