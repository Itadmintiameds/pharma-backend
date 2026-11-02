package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

// One line of the bill count summary.
@Data
@AllArgsConstructor
public class BillCountSummaryRowDto {

    // TOTAL_BILLS, FULLY_PAID_BILLS or PENDING_IP_BILLS.
    private String code;

    // Display label, e.g. Total Bills.
    private String particular;

    private long count;
}
