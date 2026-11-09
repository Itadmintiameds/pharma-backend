package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the G.P. on Consolidated Sales Book. Every list is
// a multi-select; null or empty means "no restriction" on that field.
@Data
public class GrossProfitSalesBookFilter {

    // Reporting period, both inclusive. A single month or several months are
    // just a wider range; toDate defaults to fromDate, fromDate to today.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Empty = every pharmacy the logged-in user can access (All Pharmacies).
    private List<String> pharmacyIds;

    // GST Rate: 5, 12, 18 ... matched against the product's GST percentage.
    private List<BigDecimal> gstRates;

    // DAY (default): one row per date, as on the screen.
    // MONTH: one row per month, for monthly / multi-month reporting.
    private String groupBy;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    private Boolean unpaged;

    // date, billAmount, cost, grossProfit, grossProfitPercentage.
    // Defaults to date ASC.
    private String sortBy;
    private String sortDir;
}
