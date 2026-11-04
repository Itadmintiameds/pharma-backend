package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the billing by user report.
@Data
public class BillingByUserKpiDto {

    // Bills raised on the day, across all users shown.
    private long totalBills;

    private BigDecimal totalSales;
}
