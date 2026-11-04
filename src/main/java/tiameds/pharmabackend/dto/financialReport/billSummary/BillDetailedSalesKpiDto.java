package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the bill detailed sales report. Totals of every line matching
// the filters, not just the current page.
@Data
public class BillDetailedSalesKpiDto {

    private BigDecimal totalGrossAmount;

    private BigDecimal totalDiscountAmount;

    // CGST + SGST.
    private BigDecimal totalGstAmount;

    private BigDecimal totalNetAmount;
}
