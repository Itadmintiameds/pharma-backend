package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the table's Total row, over every row matching the filters
// (not just the current page).
@Data
@JsonPropertyOrder({
        "purchaseInvoiceCount", "lineItemCount",
        "totalPurchaseAmount", "taxableAmount",
        "gstAmount", "gstSharePercentage",
        "purchasedQuantity", "freeQuantity"})
public class PurchaseRegisterReportSummaryDto {

    // Purchase Invoices card: "4", "5 line items purchased".
    private Long purchaseInvoiceCount;
    private Long lineItemCount;

    // Total Purchase Amount card: "₹16,026.50", "₹14,535.00 taxable value".
    private BigDecimal totalPurchaseAmount;
    private BigDecimal taxableAmount;

    // GST Amount card: "₹1,491.50", "9% of total amount".
    private BigDecimal gstAmount;
    private BigDecimal gstSharePercentage;

    // Purchased Quantity card: "410", "15 units received free".
    private Long purchasedQuantity;
    private Long freeQuantity;
}
