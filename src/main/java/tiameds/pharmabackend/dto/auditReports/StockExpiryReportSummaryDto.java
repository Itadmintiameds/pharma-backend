package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// KPI totals over every row matching the filters (not just the current page).
// Batch counts are location + product + batch rows; quantities are units on hand.
@Data
@JsonPropertyOrder({
        "expiredStock", "expiredBatches",
        "expiringThisMonthQuantity", "expiringThisMonthBatches",
        "expiringSoonQuantity", "expiringSoonBatches",
        "totalStock", "totalStockBatches"})
public class StockExpiryReportSummaryDto {

    private Long expiredStock;
    private Long expiredBatches;

    private Long expiringThisMonthQuantity;
    private Long expiringThisMonthBatches;

    private Long expiringSoonQuantity;
    private Long expiringSoonBatches;

    private Long totalStock;
    private Long totalStockBatches;
}
