package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI cards and the "Total Movement" row, over every row matching the filters
// (not just the current page).
//
// openingStock and closingStock are the real stock at the selected locations /
// products at the start and end of the period, so they ignore the
// movementTypes / referenceNo / sourceLocationIds / destinationLocationIds
// filters; the stock in / out totals honour them.
@Data
@JsonPropertyOrder({"openingStock", "totalStockIn", "totalStockOut", "closingStock"})
public class StockMovementReportSummaryDto {

    private Long openingStock;
    private Long totalStockIn;
    private Long totalStockOut;
    private Long closingStock;
}
