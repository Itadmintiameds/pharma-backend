package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the table's Total row, over every invoice matching the
// filters (not just the current page).
@Data
@JsonPropertyOrder({
        "totalCreditInvoiceAmount", "outstandingInvoiceCount",
        "totalSupplierPayable", "payableSharePercentage",
        "overduePayable", "overdueInvoiceCount",
        "supplierCredit",
        "totalPaid", "totalPurchaseReturnAdjusted"})
public class SupplierOutstandingReportSummaryDto {

    // Total Credit Invoice Amount card: "₹1,45,000.00", "4 invoices with
    // outstanding balance".
    private BigDecimal totalCreditInvoiceAmount;
    private Long outstandingInvoiceCount;

    // Total Supplier Payable card: "₹72,000.00", "50% of invoice amount".
    private BigDecimal totalSupplierPayable;
    private BigDecimal payableSharePercentage;

    // Overdue Payable card: "₹27,000.00", "2 invoices past due date".
    private BigDecimal overduePayable;
    private Long overdueInvoiceCount;

    // Supplier Credit card: "₹2,000.00 Amount due from suppliers".
    private BigDecimal supplierCredit;

    // Total row: Total Paid and Purchase Return Adjusted columns.
    private BigDecimal totalPaid;
    private BigDecimal totalPurchaseReturnAdjusted;
}
