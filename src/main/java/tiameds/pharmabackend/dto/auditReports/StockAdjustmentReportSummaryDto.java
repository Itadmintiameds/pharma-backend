package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI cards and the table's Total row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({
        "stockIncreaseQuantity", "stockIncreaseCount",
        "stockDecreaseQuantity", "stockDecreaseCount",
        "netAdjustmentQuantity", "totalAdjustments"})
public class StockAdjustmentReportSummaryDto {

    // Stock Increase card / Adjustment In total: "15", "2 adjustments".
    private Long stockIncreaseQuantity;
    private Long stockIncreaseCount;

    // Stock Decrease card / Adjustment Out total: "38", "4 adjustments".
    private Long stockDecreaseQuantity;
    private Long stockDecreaseCount;

    // Net Adjustment card / Net Adjustment total: "-23", "6 adjustments".
    private Long netAdjustmentQuantity;
    private Long totalAdjustments;
}
