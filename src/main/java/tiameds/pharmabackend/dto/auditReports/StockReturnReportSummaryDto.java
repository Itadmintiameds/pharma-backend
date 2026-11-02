package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI cards and the table's Total row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({"totalReturnedQuantity", "damageQuantity", "expiryQuantity"})
public class StockReturnReportSummaryDto {

    private Long totalReturnedQuantity;
    private Long damageQuantity;
    private Long expiryQuantity;
}
