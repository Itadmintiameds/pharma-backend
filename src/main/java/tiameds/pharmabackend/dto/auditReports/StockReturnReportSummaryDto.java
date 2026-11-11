package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the table's Total row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({
        "totalReturnedQuantity", "totalReturns",
        "damageQuantity", "damagePercentage",
        "expiryQuantity", "expiryPercentage"})
public class StockReturnReportSummaryDto {

    private Long totalReturnedQuantity;

    // "6 returns in this period": distinct return documents.
    private Long totalReturns;

    private Long damageQuantity;

    // "55% of returned quantity".
    private BigDecimal damagePercentage;

    private Long expiryQuantity;
    private BigDecimal expiryPercentage;
}
