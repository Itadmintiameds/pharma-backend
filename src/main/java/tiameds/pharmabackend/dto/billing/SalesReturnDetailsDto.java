package tiameds.pharmabackend.dto.billing;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesReturnDetailsDto {

    private Long salesReturnDetailId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;

    // In smallest units, the same unit as BillingDetails.billQuantity.
    private Long salesReturnQuantity;
    private String salesReturnReason;

    // Read-only: priced off the original bill line, ignored on input.
    private BigDecimal grossAmount;
    private BigDecimal gstAmount;
    private BigDecimal netAmount;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
