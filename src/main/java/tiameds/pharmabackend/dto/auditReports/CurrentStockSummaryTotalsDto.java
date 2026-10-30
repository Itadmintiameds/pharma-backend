package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI totals over every row matching the filters (not just the current page).
@Data
@JsonPropertyOrder({
        "totalClosingStock", "totalDamagedStock", "totalExpiredStock", "totalAvailableStock"})
public class CurrentStockSummaryTotalsDto {

    private Long totalClosingStock;
    private Long totalDamagedStock;
    private Long totalExpiredStock;
    private Long totalAvailableStock;
}
