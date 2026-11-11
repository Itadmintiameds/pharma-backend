package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Adjustment Type Summary table: quantity per adjustment type and the
// "Net Adjustment" row (increase - decrease), over every row matching the
// filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "netAdjustmentQuantity"})
public class StockAdjustmentReportTypeSummaryDto {

    private List<StockAdjustmentReportTypeSummaryRowDto> rows;
    private Long netAdjustmentQuantity;
}
