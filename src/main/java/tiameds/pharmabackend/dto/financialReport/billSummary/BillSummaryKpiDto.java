package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the bill summary report. Computed over every bill matching the
// filters, not just the current page.
@Data
public class BillSummaryKpiDto {

    // Sum of the collected (rounded-off) amount of the matching bills.
    private BigDecimal totalSalesAmount;

    // Sum of Completed sales returns dated within the date filter, raised
    // against bills that match the search / referred-by filters.
    private BigDecimal totalSalesReturnAmount;

    // totalSalesAmount - totalSalesReturnAmount.
    private BigDecimal netSalesAmount;
}
