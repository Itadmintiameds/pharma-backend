package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Gross Profit / Margin Analysis Report. Every
// list is a multi-select and every search is a case-insensitive "contains";
// null or empty means "no restriction" on that field.
@Data
public class GrossProfitReportFilter {

    // Sale / return date period, both inclusive. Today / Specific Day /
    // By Month / By Date Range on the screen all resolve to this pair on the
    // client.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Empty = every pharmacy the logged-in user can access.
    private List<String> pharmacyIds;

    // Product Name: pick products by id, and/or free-text search.
    private List<String> productIds;
    private String productName;

    private String batchNumber;

    // Pharmacy bill reference. Returns are matched on their original bill.
    private String billNo;

    // Completed sales return reference, e.g. SLR-2026-00001. Narrows the
    // report to the bills that return was made against.
    private String salesReturnNo;

    // Customer/Patient Type: WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE.
    private List<String> customerTypes;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // pharmacyName, productName, batchNumber, soldQuantity, returnQuantity,
    // netQuantity, netSalesAmount, netCost, grossProfit, marginPercentage.
    // Defaults to grossProfit DESC.
    private String sortBy;
    private String sortDir;
}
