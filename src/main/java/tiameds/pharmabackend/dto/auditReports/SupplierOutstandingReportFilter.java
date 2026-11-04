package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Supplier Outstanding report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class SupplierOutstandingReportFilter {

    // Reporting period on the original purchase invoice date, both days
    // inclusive. The Today / Specific Day / By Month / By Date Range tabs and
    // the Purchase Invoice Date filter all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Locations, decided by the organization: a multi-pharmacy organization
    // with centralized inventory reports on warehouseIds, a single pharmacy or
    // multi-pharmacy without a warehouse on pharmacyIds. Sending the other
    // kind is rejected. Empty = every location of that kind the user can access.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Supplier: supplier master ids (multi-select) and / or a free-text
    // search on the supplier name.
    private List<Long> supplierIds;
    private String supplierSearch;

    // Purchase Invoice No. / GRN No.: free-text search.
    private String invoiceNumber;
    private String grnNumber;

    // Payment Due Date range (invoice date + credit days), both days
    // inclusive. Either end may be left open.
    private LocalDate dueFromDate;
    private LocalDate dueToDate;

    // Outstanding Status: ALL, OUTSTANDING_ONLY (Unpaid + Partially Settled),
    // UNPAID, PARTIALLY_SETTLED, FULLY_SETTLED.
    private String outstandingStatus;

    // Outstanding Amount: min / max Current Outstanding, both inclusive.
    private BigDecimal minOutstanding;
    private BigDecimal maxOutstanding;

    // Supplier Credit: ALL, CREDIT (returns / payments exceed the invoice),
    // WITHOUT_CREDIT.
    private String supplierCredit;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // supplierName, invoiceNumber, grnNumber, invoiceDate, dueDate,
    // invoiceAmount, totalPaid, purchaseReturnAdjusted, currentOutstanding,
    // overdueDays, supplierCredit. Defaults to currentOutstanding DESC.
    private String sortBy;
    private String sortDir;
}
