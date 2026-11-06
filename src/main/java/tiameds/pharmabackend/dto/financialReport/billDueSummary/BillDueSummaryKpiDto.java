package tiameds.pharmabackend.dto.financialReport.billDueSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the bill due summary. Totals of every IP bill matching the
// filters, not just the current page.
@Data
public class BillDueSummaryKpiDto {

    private BigDecimal totalIpBillAmount;

    private BigDecimal totalAmountReceived;

    // Completed sales returns against these bills.
    private BigDecimal totalReturnedAmount;

    // Sum of each bill's pending amount (bill - received - returned, never
    // below zero per bill).
    private BigDecimal totalPendingAmount;
}
