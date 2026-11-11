package tiameds.pharmabackend.controller.auditReports;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.GrossProfitReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
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
            @RequestParam(required = false) Boolean unpaged,

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
        filter.setUnpaged(unpaged);

        GrossProfitReportResponse report =
                grossProfitReportService.getGrossProfitReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Gross Profit / Margin Analysis Report:
     *   /auditReports/gross-profit/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give. Written straight to the
     * response, so the file streams out as it is built.
     */
    @GetMapping("/gross-profit/export")
    public void exportGrossProfitReport(
            @RequestParam String format,
            @ModelAttribute GrossProfitReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                grossProfitReportService.exportGrossProfitReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
