package tiameds.pharmabackend.dto.financialReport.pharmacyReceipts;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One payment mode in the payment mode summary, or the TOTAL line.
@Data
@AllArgsConstructor
public class PaymentModeSummaryRowDto {

    // CARD, CASH, UPI, CREDIT or TOTAL.
    private String code;

    // Display label, e.g. Card.
    private String paymentMode;

    private BigDecimal amountReceived;
}
