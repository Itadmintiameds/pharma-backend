package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI cards and the table's Total row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({"returnedQuantity", "adjustedQuantity", "totalDamagedQuantity"})
public class StockDamageReportSummaryDto {

    private Long returnedQuantity;
    private Long adjustedQuantity;
    private Long totalDamagedQuantity;
}
