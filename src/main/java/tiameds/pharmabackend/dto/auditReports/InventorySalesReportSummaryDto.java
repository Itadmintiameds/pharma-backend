package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI totals over every row matching the filters (not just the current page).
@Data
@JsonPropertyOrder({"totalSaleAmount", "totalRoundOffAmount", "totalSaleAmountAfterRoundOff",
        "totalReturnAmount", "totalNetSales", "totalNetSaleQuantity"})
public class InventorySalesReportSummaryDto {

    private BigDecimal totalSaleAmount;
    // Bills' round-off, shared across their lines by amount.
    private BigDecimal totalRoundOffAmount;
    // totalSaleAmount + totalRoundOffAmount — the whole-rupee amount billed.
    private BigDecimal totalSaleAmountAfterRoundOff;
    private BigDecimal totalReturnAmount;
    private BigDecimal totalNetSales;
    private Long totalNetSaleQuantity;
}
