package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Stock Damage report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class StockDamageReportFilter {

    // Reporting period on the transaction date, both days inclusive. The
    // Today / Specific Day / By Month / By Date Range tabs all map onto these.
    // Missing fromDate = today; missing toDate = fromDate.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind. A stock
    // return is in scope through its source pharmacy or its destination
    // warehouse.
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

    // Supplier: supplier on the purchase invoice the batch came in on.
    private List<Long> supplierIds;

    // STOCK_RETURN, STOCK_ADJUSTMENT (labels "Stock Return" also accepted).
    // Empty or ALL = both.
    private List<String> transactionTypes;

    // Reference Number: free-text search on the return / adjustment number.
    private String referenceNumber;

    // Destination: warehouses (stock returns from a pharmacy) and suppliers
    // (purchase returns). Adjustments have no destination, so sending either
    // list leaves them out.
    private List<String> destinationWarehouseIds;
    private List<Long> destinationSupplierIds;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // transactionDate, productName, batchNo, expiryDate, transactionType,
    // referenceNumber, source, destination, returnedQuantity,
    // adjustedQuantity, totalDamagedQuantity. Defaults to transactionDate DESC.
    private String sortBy;
    private String sortDir;
}
