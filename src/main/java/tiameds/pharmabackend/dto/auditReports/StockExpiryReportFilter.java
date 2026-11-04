package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// Query-param filters for the Stock Expiry report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class StockExpiryReportFilter {

    // Expiry status, days to expiry, "this month" and Expiry Within are all
    // worked out relative to this day. Defaults to today.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate asOfDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Product Name: pick products by id or by exact name (case-insensitive).
    private List<String> productIds;
    private List<String> productNames;

    private List<Long> productCategoryIds;

    // Brand Name: exact brand names (case-insensitive).
    private List<String> brandNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Supplier Name: suppliers on the purchase invoice the batch came in on.
    private List<Long> supplierIds;

    // ALL, EXPIRED, EXPIRING_THIS_MONTH, EXPIRING_SOON, VALID.
    private String expiryStatus;

    // Expiry Within: batches not yet expired that expire within this many
    // months (dropdown 1 / 2 / 3 / 6) or days (Custom). Send at most one.
    private Integer expiryWithinMonths;
    private Integer expiryWithinDays;

    // Expiry Month/Year range on the batch, both months inclusive (yyyy-MM).
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth expiryFromMonth;

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth expiryToMonth;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // locationName, productName, batchNo, supplierName, expiryDate,
    // monthsToExpiry, quantity, damagedQuantity. Defaults to expiryDate ASC.
    private String sortBy;
    private String sortDir;
}
