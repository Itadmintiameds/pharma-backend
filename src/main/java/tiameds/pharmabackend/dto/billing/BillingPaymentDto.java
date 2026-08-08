package tiameds.pharmabackend.dto.billing;

import lombok.Data;
import tiameds.pharmabackend.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingPaymentDto {

    private Long paymentId;
    private Long billingId;
    private PaymentMode paymentMode;
    private String transactionId;
    private BigDecimal receivedAmount;
    private BigDecimal pendingAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
