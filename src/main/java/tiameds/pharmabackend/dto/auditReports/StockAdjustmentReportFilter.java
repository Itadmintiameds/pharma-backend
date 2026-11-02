package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Stock Adjustment report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class StockAdjustmentReportFilter {

    // Reporting period on the adjustment date, both days inclusive. The
    // Today / Specific Day / By Month / By Date Range tabs all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Not sent = every pharmacy the user can access. Warehouse adjustments
    // are not part of this report.
    private List<String> pharmacyIds;

    // Product Name: exact product names (case-insensitive).
    private List<String> productNames;

    private List<Long> productCategoryIds;

    // Brand Name: exact brand names (case-insensitive).
    private List<String> brandNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Supplier: supplier on the purchase invoice the batch came in on.
    private List<Long> supplierIds;

    // INCREASE or DECREASE (labels "Stock Increase" / "Stock Decrease" also
    // accepted). Empty or ALL = both.
    private String adjustmentType;

    // Adjustment Reason: exact reasons (case-insensitive), as listed by
    // GET /auditReports/stock-adjustment/reasons.
    private List<String> adjustmentReasons;

    // Stock Adjustment Reference No.: free-text search on the adjustment number.
    private String referenceNumber;

    // Adjusted By: user ids of the users who recorded the adjustment.
    private List<String> adjustedByIds;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // adjustmentDate, pharmacyName, referenceNumber, productName, batchNo,
    // expiryDate, adjustmentType, adjustmentReason, stockBefore, adjustmentIn,
    // adjustmentOut, netAdjustment, stockAfter, adjustedBy. Defaults to
    // adjustmentDate DESC.
    private String sortBy;
    private String sortDir;
}
