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
@Data
public class FinancialReportFilter {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Matches patient / customer name or mobile number (partial, case-insensitive).
    private String patientSearch;

    // Matches patient number, IP / OP number or bill number (partial, case-insensitive).
    private String numberSearch;

    // "Referred by" multi-select: doctor ids from /doctor/allDoctor.
    private List<Long> doctorIds;

    // Zero-based page index and page size, both chosen by the client.
    private Integer page;
    private Integer size;
}
