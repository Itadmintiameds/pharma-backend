package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// KPI cards and the table's Total row, over every payment matching the
// filters (not just the current page).
@Data
@JsonPropertyOrder({
        "totalPaymentAmount", "paymentCount",
        "averagePaymentAmount",
        "suppliersPaidCount", "suppliersPaid",
        "invoicesPaidAgainstCount", "topInvoiceNumber", "topInvoiceInstalments"})
public class SupplierPaymentReportSummaryDto {

    // Total Payment Amount card: "₹60,000.00", "4 payments in this period".
    // Payment Transactions card: "4", "₹15,000 average payment".
    private BigDecimal totalPaymentAmount;
    private Long paymentCount;
    private BigDecimal averagePaymentAmount;

    // Suppliers Paid card: "3", "A, B, C received payments".
    private Long suppliersPaidCount;
    private List<String> suppliersPaid;

    // Invoices Paid Against card: "3", "PI-1001 paid in 2 instalments" -
    // the invoice with the most payments in the period (null when none).
    private Long invoicesPaidAgainstCount;
    private String topInvoiceNumber;
    private Long topInvoiceInstalments;
}
