package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Stock Movement Report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class StockMovementReportFilter {

    // Reporting period, both inclusive. Today / Specific Day / By Month /
    // By Date Range all map onto this pair. Defaults to today.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Locations. Neither sent = every pharmacy and warehouse the user can
    // access; only one sent = just those locations of that kind.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Product Name: pick products by id or by exact name (case-insensitive).
    private List<String> productIds;
    private List<String> productNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Movement Type codes: PURCHASE_RECEIPT, ALLOCATION_IN, ALLOCATION_OUT,
    // STOCK_TRANSFER_IN, STOCK_TRANSFER_OUT, PHARMACY_SALE, SALES_RETURN,
    // PURCHASE_RETURN, DAMAGE_CAPTURE, DAMAGE_RETURN, EXPIRED_STOCK_RETURN,
    // STOCK_ADJUSTMENT.
    private List<String> movementTypes;

    // Reference No.: free-text search on the transaction reference number.
    private String referenceNo;

    // Source / Destination Location: pharmacy or warehouse ids on either side
    // of an internal movement (allocation, transfer, warehouse return).
    private List<String> sourceLocationIds;
    private List<String> destinationLocationIds;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // transactionDate, movementType, referenceNo, locationName, productName,
    // batchNo, source, destination, stockIn, stockOut, balanceQuantity.
    // Defaults to transactionDate ASC.
    private String sortBy;
    private String sortDir;
}
