package tiameds.pharmabackend.dto.financialReport.common;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// Query parameters shared by every financial report. Bound from the request
// query string, e.g.
//   ?date=2026-10-28
//   ?fromDate=2026-10-01&toDate=2026-10-31&doctorIds=3&doctorIds=7&page=0&size=20
//
// Date filter: send either a single `date` (today / a specific day) or a
// `fromDate` / `toDate` range (a month is just its first to last day). When
// `date` is present the range is ignored. Either end of the range may be left
// out to make it open-ended; with no dates at all, every record is included.
//
// Text searches are partial and case-insensitive. Multi-selects are exact
// matches and repeat the parameter (or comma separate): gstPercentages=12,18.
// A report ignores any filter that does not apply to it.
@Data
public class FinancialReportFilter {

    // Pharmacy multi-select. Each must be one the user is mapped to. When
    // empty, the report covers the pharmacy in the X-Pharmacy-Id header.
    private List<String> pharmacyIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Patient / customer name or mobile number.
    private String patientSearch;

    // One box matching patient number, IP / OP number or bill number.
    private String numberSearch;

    // Individual number searches.
    private String patientNo;
    private String ipNo;
    private String opNo;
    private String billNo;

    // "Referred by" multi-select: doctor ids from /doctor/allDoctor.
    private List<Long> doctorIds;

    // Patient type multi-select: IP, OP, DAY_CARE.
    private List<String> patientTypes;

    // Product name search and product multi-select (line-level reports only).
    private String productSearch;
    private List<String> productIds;

    // HSN code search and multi-select (line-level reports only).
    private String hsnSearch;
    private List<String> hsnCodes;

    // GST % multi-select, as stored on the product, e.g. 5, 12, 18
    // (line-level reports only).
    private List<String> gstPercentages;

    // Payment status dropdown on due reports: PENDING, FULLY_PAID or ALL
    // (default).
    private String pendingStatus;

    // Zero-based page index and page size, both chosen by the client.
    private Integer page;
    private Integer size;
}
