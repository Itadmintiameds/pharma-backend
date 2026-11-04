package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One row of the Payment Mode-wise Summary table. Every mode is listed,
// with 0 transactions when it has none.
@Data
@JsonPropertyOrder({"paymentMode", "transactionCount", "totalAmount"})
public class SupplierPaymentModeSummaryDto {

    // Cash / UPI / Bank Transfer / Cheque / Other.
    private String paymentMode;
    private Long transactionCount;
    private BigDecimal totalAmount;
}
