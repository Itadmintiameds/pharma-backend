package tiameds.pharmabackend.dto.financialReport.billDueSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the bill due summary. Totals of every IP bill matching the
// filters, not just the current page.
@Data
public class BillDueSummaryKpiDto {

    private BigDecimal totalIpBillAmount;

    private BigDecimal totalAmountReceived;

    // totalIpBillAmount - totalAmountReceived.
    private BigDecimal totalPendingAmount;
}
