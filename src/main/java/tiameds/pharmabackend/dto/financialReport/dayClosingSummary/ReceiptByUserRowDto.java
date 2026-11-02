package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One line of the receipt by user report: what a user collected in one
// pharmacy, or the closing total (code TOTAL, no pharmacy or user).
@Data
@AllArgsConstructor
public class ReceiptByUserRowDto {

    // USER or TOTAL.
    private String code;

    private String pharmacyId;
    private String pharmacyName;

    // User who recorded the payments; the name falls back to the id when not set.
    private String userId;
    private String userName;

    // Number of payment transactions (credit entries excluded).
    private long receiptCount;

    private BigDecimal amountReceived;
}
