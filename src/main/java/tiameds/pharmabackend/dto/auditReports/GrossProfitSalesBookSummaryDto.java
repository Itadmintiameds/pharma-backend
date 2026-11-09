package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI tiles and the table's "Total" row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({
        "totalBillAmount", "daysWithSales",
        "totalCost", "costPercentageOfBillAmount",
        "totalGrossProfit", "averageGrossProfitPerDay",
        "overallGrossProfitPercentage"})
public class GrossProfitSalesBookSummaryDto {

    // Total Bill Amount, "21 days with sales".
    private BigDecimal totalBillAmount;
    private Long daysWithSales;

    // Total Cost, "36% of bill amount".
    private BigDecimal totalCost;
    private BigDecimal costPercentageOfBillAmount;

    // Total Gross Profit, "27,256 average per day" (per day with sales).
    private BigDecimal totalGrossProfit;
    private BigDecimal averageGrossProfitPerDay;

    // Overall G.P. % = total gross profit / total bill amount.
    private BigDecimal overallGrossProfitPercentage;
}
