package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDetailsDto {

    private Long purchaseDetailsId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private Long purchaseQuantity;
    private Long freeUnit;
    private Long freeQuantity;
    private BigDecimal grossAmount;
    private BigDecimal gst;
    private BigDecimal netAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
