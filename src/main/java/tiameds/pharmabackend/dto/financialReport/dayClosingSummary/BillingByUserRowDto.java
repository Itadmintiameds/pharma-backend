package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One line of the billing by user report: a user's bills in one pharmacy, or
// the closing total (code TOTAL, no pharmacy or user).
@Data
@AllArgsConstructor
public class BillingByUserRowDto {

    // USER or TOTAL.
    private String code;

    private String pharmacyId;
    private String pharmacyName;

    // User who raised the bills; the name falls back to the id when not set.
    private String userId;
    private String userName;

    private long billCount;

    // Sum of the bills' total amount.
    private BigDecimal totalSales;
}
