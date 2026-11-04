package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the Report Summary table, over every line matching the
// filters (not just the current page). Also serves the Total rows of both
// tables.
@Data
@JsonPropertyOrder({
        "totalSalesReturnTransactions", "totalReturnLines", "totalReturnQuantity",
        "totalHistoricalAmount", "totalDiscountReversed",
        "totalTaxableAmountReversed", "totalGstReversed",
        "totalReturnAmount", "totalOutstandingAdjustment", "totalRefundAmount",
        "outstandingAdjustmentPercentage", "refundPercentage"})
public class PharmacySalesReturnReportSummaryDto {

    private Long totalSalesReturnTransactions;

    // Product / batch lines returned.
    private Long totalReturnLines;
    private Long totalReturnQuantity;

    private BigDecimal totalHistoricalAmount;
    private BigDecimal totalDiscountReversed;
    private BigDecimal totalTaxableAmountReversed;
    private BigDecimal totalGstReversed;

    private BigDecimal totalReturnAmount;
    private BigDecimal totalOutstandingAdjustment;
    private BigDecimal totalRefundAmount;

    // Share of totalReturnAmount, 0-100: "42% of return amount", "58% paid back".
    private BigDecimal outstandingAdjustmentPercentage;
    private BigDecimal refundPercentage;
}
