package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Product-wise Purchase and Sales Analysis Report.
// Every list is a multi-select; null or empty means "no restriction".
@Data
public class ProductWiseReportFilter {

    // Reporting period, both inclusive. Today / Specific Day / By Month /
    // By Date Range on the screen all resolve to this pair on the client.
    private LocalDate fromDate;
    private LocalDate toDate;

    // Pharmacy / warehouse scope. Both empty = every pharmacy and warehouse
    // the logged-in user can access; purchases received at a warehouse only
    // show when the warehouse is in scope.
    private List<String> pharmacyIds;
    private List<String> warehouseIds;

    // Product Name: pick products by id or by exact name (case-insensitive).
    private List<String> productIds;
    private List<String> productNames;

    // Batch No.: free-text search on the batch number.
    private String batchNo;

    // Supplier Name: supplier master ids. Purchases and supplier returns are
    // narrowed to the supplier; sales to batches bought from that supplier.
    private List<Long> supplierIds;

    private List<Long> productCategoryIds;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // productName, batchNo, purchaseQuantity, supplierReturnQuantity,
    // netPurchaseQuantity, saleQuantity, salesReturnQuantity, netSaleQuantity,
    // purchaseAmount, supplierReturnAmount, netPurchaseAmount, saleAmount,
    // salesReturnAmount, netSaleAmount. Defaults to productName ASC.
    private String sortBy;
    private String sortDir;
}
