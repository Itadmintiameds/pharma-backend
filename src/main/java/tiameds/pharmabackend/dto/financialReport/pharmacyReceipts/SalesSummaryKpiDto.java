package tiameds.pharmabackend.dto.financialReport.pharmacyReceipts;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the sales summary.
@Data
public class SalesSummaryKpiDto {

    private BigDecimal totalSales;

    private BigDecimal totalSalesReturn;

    // totalSales - totalSalesReturn.
    private BigDecimal netSales;

    private BigDecimal ipPendingAmount;
}
