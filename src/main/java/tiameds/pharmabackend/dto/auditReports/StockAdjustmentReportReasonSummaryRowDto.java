package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One row of the Adjustment Reason Summary table:
// "Stock Correction", in 15, out 5, net +10.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"adjustmentReason", "adjustmentIn", "adjustmentOut", "netAdjustment"})
public class StockAdjustmentReportReasonSummaryRowDto {

    private String adjustmentReason;
    private Long adjustmentIn;
    private Long adjustmentOut;
    private Long netAdjustment;
}
