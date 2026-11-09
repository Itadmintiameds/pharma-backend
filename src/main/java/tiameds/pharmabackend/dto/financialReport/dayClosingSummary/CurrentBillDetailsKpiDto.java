package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the current bill details report, over every matching bill
// (not just the page).
@Data
public class CurrentBillDetailsKpiDto {

    private long totalBills;

    private BigDecimal totalAmount;

    // Received on these bills up to the end of the day.
    private BigDecimal totalAmountReceived;

    // Completed returns against these bills up to the end of the day.
    private BigDecimal totalReturnedAmount;

    // Still due on the IP bills at the end of the day, after returns; summed
    // per bill so one bill's overpayment never offsets another's due.
    private BigDecimal totalPendingAmount;
}
