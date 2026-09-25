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
    private Long freeReturnQuantity;
    private String returnReason;
    private BigDecimal grossAmount;
    private BigDecimal gstAmount;
    private BigDecimal netAmount;

    // Read-only revision info; set by the server, ignored on input.
    private Integer revisionNo;
    private Boolean isActive;
    private Long previousDetailId;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
