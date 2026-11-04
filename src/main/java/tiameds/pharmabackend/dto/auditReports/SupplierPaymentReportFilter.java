package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Supplier Payment report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class SupplierPaymentReportFilter {

    // Payment Date: reporting period on the date recorded against the
    // supplier payment, both days inclusive. The Today / Specific Day /
    // By Month / By Date Range tabs map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Locations, decided by the organization: a multi-pharmacy organization
    // with centralized inventory reports on warehouseIds, a single pharmacy or
    // multi-pharmacy without a warehouse on pharmacyIds. Sending the other
    // kind is rejected. Empty = every location of that kind the user can access.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Supplier Name: supplier master ids (multi-select); empty = All.
    private List<Long> supplierIds;

    // Supplier Payment No. / Purchase Invoice No. / GRN No. / Payment
    // Reference No. (transaction / cheque / bank reference): free-text search.
    private String paymentNumber;
    private String invoiceNumber;
    private String grnNumber;
    private String referenceNumber;

    // Payment Mode: CASH, UPI, BANK_TRANSFER, CHEQUE, OTHER.
    private List<String> paymentModes;

    // Payment Amount: min / max amount of the payment, both inclusive.
    private BigDecimal minPaymentAmount;
    private BigDecimal maxPaymentAmount;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // paymentDate, paymentNumber, supplierName, invoiceNumber, grnNumber,
    // invoiceAmount, paymentAmount, paymentMode, outstandingBefore,
    // outstandingAfter. Defaults to paymentDate DESC.
    private String sortBy;
    private String sortDir;
}
