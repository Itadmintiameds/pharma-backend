package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.GrossProfitReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class GrossProfitReportController {

    private final GrossProfitReportService grossProfitReportService;

    /**
     * Gross Profit / Margin Analysis Report: net sales revenue of each
     * product + batch compared with the purchase cost of the units sold.
     * Multi-select params accept repeats (?pharmacyIds=A&pharmacyIds=B)
     * or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/GROSS_PROFIT_REPORT/VIEW')")
    @GetMapping("/gross-profit")
    public ResponseEntity<?> getGrossProfitReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String salesReturnNo,
            @RequestParam(required = false) List<String> customerTypes,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "grossProfit") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GrossProfitReportFilter filter = new GrossProfitReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setProductIds(productIds);
        filter.setProductName(productName);
        filter.setBatchNumber(batchNumber);
        filter.setBillNo(billNo);
        filter.setSalesReturnNo(salesReturnNo);
        filter.setCustomerTypes(customerTypes);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        GrossProfitReportResponse report =
                grossProfitReportService.getGrossProfitReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
