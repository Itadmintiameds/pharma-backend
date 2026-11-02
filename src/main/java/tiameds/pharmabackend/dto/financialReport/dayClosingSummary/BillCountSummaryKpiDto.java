package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.Data;

// KPI cards for the bill count summary.
@Data
public class BillCountSummaryKpiDto {

    // Bills raised on the business day.
    private long totalBills;

    // Of those, bills fully settled by the end of the day.
    private long fullyPaidBills;

    // Of those, IP bills with an amount still due at the end of the day.
    private long pendingIpBills;
}
