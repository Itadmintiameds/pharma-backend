package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards, over every row matching the filters (not just the current page).
@Data
@JsonPropertyOrder({
        "purchaseReturnCount", "purchaseInvoiceCount",
        "totalReturnAmount", "taxableAmount",
        "gstReversalAmount", "gstSharePercentage",
        "totalPhysicalReturnQuantity", "freeReturnQuantity",
        "returnQuantity", "grossAmount", "discountAmount"})
public class SupplierPurchaseReturnReportSummaryDto {

    // Confirmed Purchase Returns card: "5", "5 invoices returned against".
    private Long purchaseReturnCount;
    private Long purchaseInvoiceCount;

    // Total Return Amount card: "₹3,334.31", "₹3,002.00 taxable amount".
    private BigDecimal totalReturnAmount;
    private BigDecimal taxableAmount;

    // GST Reversal Amount card: "₹332.31", "10% of return amount".
    private BigDecimal gstReversalAmount;
    private BigDecimal gstSharePercentage;

    // Total Physical Return Qty card: "69", "4 free units included".
    // Return quantity + free return quantity.
    private Long totalPhysicalReturnQuantity;
    private Long freeReturnQuantity;

    // Table Total row and the Supplier Purchase Return Summary table:
    // Return Purchase Qty "65", Gross Return Amount "₹3,160.00", Discount
    // "₹158.00". grossAmount - discountAmount = taxableAmount.
    private Long returnQuantity;
    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
}
