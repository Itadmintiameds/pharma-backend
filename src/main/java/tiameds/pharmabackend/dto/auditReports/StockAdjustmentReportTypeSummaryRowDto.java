package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One row of the Adjustment Type Summary table: INCREASE / "Stock Increase", 15.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "quantity"})
public class StockAdjustmentReportTypeSummaryRowDto {

    private String code;
    private String label;
    private Long quantity;
}
