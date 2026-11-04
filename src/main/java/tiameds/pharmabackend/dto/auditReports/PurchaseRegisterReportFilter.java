package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Purchase Register report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class PurchaseRegisterReportFilter {

    // Reporting period on the purchase invoice date, both days inclusive. The
    // Today / Specific Day / By Month / By Date Range tabs all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind. Pharmacy =
    // direct pharmacy purchases, warehouse = centrally managed purchases.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Supplier: supplier master ids.
    private List<Long> supplierIds;

    // Purchase Invoice No.: free-text search on the supplier invoice number.
    private String invoiceNumber;

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

    // GST Rate: 0, 5, 12, 18, 28 ... ("Exempted" lines count as 0).
    private List<BigDecimal> gstPercentages;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // purchaseDate, invoiceNumber, supplierName, locationName, productName,
    // batchNo, expiryDate, purchasedQuantity, freeQuantity, purchaseRate,
    // grossAmount, discount, taxableAmount, gstPercentage, gstAmount,
    // totalAmount. Defaults to purchaseDate DESC.
    private String sortBy;
    private String sortDir;
}
