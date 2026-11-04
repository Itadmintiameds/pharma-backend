package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Stock Return report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class StockReturnReportFilter {

    // Reporting period on the return date, both days inclusive. The
    // Today / Specific Day / By Month / By Date Range tabs all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind. A
    // pharmacy -> warehouse return is in scope through its source pharmacy or
    // its destination warehouse; a return to a supplier through its source.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Product Name: exact product names (case-insensitive).
    private List<String> productNames;

    private List<Long> productCategoryIds;

    // Brand Name: exact brand names (case-insensitive).
    private List<String> brandNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Supplier: the supplier returned to, or for a return to a warehouse the
    // supplier on the purchase invoice the batch came in on.
    private List<Long> supplierIds;

    // PHARMACY_TO_SUPPLIER, PHARMACY_TO_WAREHOUSE, WAREHOUSE_TO_SUPPLIER
    // (labels "Pharmacy → Warehouse" / "Pharmacy -> Warehouse" also accepted).
    // Empty or ALL = every type.
    private List<String> returnTypes;

    // DAMAGE, EXPIRY, OTHER; any other value matches the entered reason text
    // exactly (case-insensitive). Empty or ALL = every reason.
    private List<String> returnReasons;

    // Stock Return Reference No.: free-text search on the return number.
    private String referenceNumber;

    // Source Location: PHARMACY / WAREHOUSE.
    private List<String> sourceLocationTypes;

    // Destination: SUPPLIER / WAREHOUSE.
    private List<String> destinationTypes;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // returnDate, referenceNumber, returnType, source, destination,
    // productName, batchNo, expiryDate, returnReason, returnedQuantity.
    // Defaults to returnDate DESC.
    private String sortBy;
    private String sortDir;
}
