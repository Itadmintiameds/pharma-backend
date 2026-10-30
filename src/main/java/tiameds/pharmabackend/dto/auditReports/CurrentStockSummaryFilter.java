package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Current Stock Summary report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class CurrentStockSummaryFilter {

    // Stock is reported as it stood at the end of this day. Defaults to today.
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

    // Expiry Date range on the batch, both inclusive.
    private LocalDate expiryFromDate;
    private LocalDate expiryToDate;

    // ALL, IN_STOCK, OUT_OF_STOCK, DAMAGED_STOCK_HELD, EXPIRED_STOCK_HELD.
    private String stockStatus;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // locationName, productName, batchNo, expiryDate, closingStock,
    // damagedStock, expiredStock, availableStock. Defaults to availableStock DESC.
    private String sortBy;
    private String sortDir;
}
