package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Supplier Purchase Return report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class SupplierPurchaseReturnReportFilter {

    // Reporting period on the purchase return date, both days inclusive. The
    // Today / Specific Day / By Month / By Date Range tabs all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind. Pharmacy =
    // single-pharmacy returns, warehouse = centrally managed returns.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Supplier: supplier master ids, taken from the original purchase invoice.
    private List<Long> supplierIds;

    // Purchase Return No.: free-text search on the return number.
    private String purchaseReturnNo;

    // Purchase Invoice No.: free-text search on the original invoice number.
    private String purchaseInvoiceNo;

    // Product Name: exact product names (case-insensitive).
    private List<String> productNames;

    private List<Long> productCategoryIds;

    // Brand Name: exact brand names (case-insensitive).
    private List<String> brandNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Expiry Month/Year range, "MM/yyyy" (or "yyyy-MM"), both months
    // inclusive. Either end may be left open.
    private String expiryFrom;
    private String expiryTo;

    // Purchase Return Status: DRAFT / CONFIRMED / CANCELLED. Not sent =
    // CONFIRMED (the screen's default); ALL = every status.
    private List<String> statuses;

    // GST Rate: 0, 5, 12, 18, 28 ... ("Exempted" lines count as 0).
    private List<BigDecimal> gstPercentages;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // returnDate, purchaseReturnNo, purchaseInvoiceNo, supplierName,
    // locationName, productName, batchNo, expiryDate, returnReason,
    // returnQuantity, freeReturnQuantity, returnRate, grossAmount, discount,
    // taxableAmount, gstPercentage, gstAmount, totalAmount, status.
    // Defaults to returnDate DESC.
    private String sortBy;
    private String sortDir;
}
