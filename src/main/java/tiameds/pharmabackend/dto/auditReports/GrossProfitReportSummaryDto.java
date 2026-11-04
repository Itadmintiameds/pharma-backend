package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the Total row of the Product Profitability table, over every
// line matching the filters (not just the current page).
@Data
@JsonPropertyOrder({
        "totalSoldQuantity", "totalReturnQuantity", "totalNetQuantity",
        "netSalesAmount", "netCostOfGoodsSold", "costPercentage",
        "grossProfit", "grossMargin",
        "totalProducts", "profitableProducts", "lossMakingProducts"})
public class GrossProfitReportSummaryDto {

    private Long totalSoldQuantity;
    private Long totalReturnQuantity;

    // "1,065 units net sold".
    private Long totalNetQuantity;

    private BigDecimal netSalesAmount;
    private BigDecimal netCostOfGoodsSold;

    // Share of netSalesAmount, 0-100: "77% of net sales".
    private BigDecimal costPercentage;

    private BigDecimal grossProfit;

    // Consolidated grossProfit / netSalesAmount, 0-100.
    private BigDecimal grossMargin;

    // Distinct products, profit summed over their batches:
    // "4 products all profitable".
    private Long totalProducts;
    private Long profitableProducts;
    private Long lossMakingProducts;
}
