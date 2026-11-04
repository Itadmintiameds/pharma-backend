package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Inventory Sales Report. Every list is a
// multi-select; null or empty means "no restriction" on that field.
@Data
public class InventorySalesReportFilter {

    // Reporting period, both inclusive. Today / Specific Day / By Month /
    // By Date Range on the screen all resolve to this pair on the client.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Empty = every pharmacy the logged-in user can access.
    private List<String> pharmacyIds;

    // Product Name: pick products by id or by exact name (case-insensitive).
    private List<String> productIds;
    private List<String> productNames;

    private List<Long> productCategoryIds;

    // HSN Code: pick codes, and/or free-text prefix search.
    private List<String> hsnCodes;
    private String hsnSearch;

    private List<BigDecimal> gstPercentages;

    // Referred By (doctor). Walk-in bills carry no doctor, so they drop out
    // whenever this is set.
    private List<Long> doctorIds;

    // Patient Type: WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE.
    private List<String> patientTypes;

    // Schedule Molecules: H, H1, X ... as stored on the molecule master.
    private List<String> drugSchedules;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // productName, saleQuantity, returnQuantity, netSaleQuantity,
    // saleAmount, returnAmount, netSales. Defaults to netSales DESC.
    private String sortBy;
    private String sortDir;
}
