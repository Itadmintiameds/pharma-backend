package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Adjustment Reason Summary table: in / out / net per reason and the Total
// row, over every row matching the filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "totalAdjustmentIn", "totalAdjustmentOut", "totalNetAdjustment"})
public class StockAdjustmentReportReasonSummaryDto {

    private List<StockAdjustmentReportReasonSummaryRowDto> rows;
    private Long totalAdjustmentIn;
    private Long totalAdjustmentOut;
    private Long totalNetAdjustment;
}
