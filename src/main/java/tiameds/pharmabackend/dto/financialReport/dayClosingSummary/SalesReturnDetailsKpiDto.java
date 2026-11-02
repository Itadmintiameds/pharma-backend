package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the sales return details report, over every matching return
// (not just the page). totalReturnAmount is the grid's Total line.
@Data
public class SalesReturnDetailsKpiDto {

    private long totalReturns;

    private BigDecimal totalReturnAmount;
}
