package tiameds.pharmabackend.controller.auditReports;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.SupplierOutstandingReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class SupplierOutstandingReportController {

    private final SupplierOutstandingReportService supplierOutstandingReportService;

    /**
     * Supplier Outstanding Report: supplier-wise and purchase invoice-wise
     * amounts still payable on credit purchases after supplier payments and
     * confirmed purchase returns. Multi-select params accept repeats
     * (?supplierIds=1&supplierIds=2) or a comma list (?supplierIds=1,2).
     */
    @PreAuthorize("@access.has('REPORTS/SUPPLIER_OUTSTANDING_REPORT/VIEW')")
    @GetMapping("/supplier-outstanding")
    public ResponseEntity<?> getSupplierOutstandingReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) String supplierSearch,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String grnNumber,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueToDate,

            @RequestParam(defaultValue = "ALL") String outstandingStatus,
            @RequestParam(required = false) BigDecimal minOutstanding,
            @RequestParam(required = false) BigDecimal maxOutstanding,
            @RequestParam(defaultValue = "ALL") String supplierCredit,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "currentOutstanding") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierOutstandingReportFilter filter = new SupplierOutstandingReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setSupplierIds(supplierIds);
        filter.setSupplierSearch(supplierSearch);
        filter.setInvoiceNumber(invoiceNumber);
        filter.setGrnNumber(grnNumber);
        filter.setDueFromDate(dueFromDate);
        filter.setDueToDate(dueToDate);
        filter.setOutstandingStatus(outstandingStatus);
        filter.setMinOutstanding(minOutstanding);
        filter.setMaxOutstanding(maxOutstanding);
        filter.setSupplierCredit(supplierCredit);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        SupplierOutstandingReportResponse report =
                supplierOutstandingReportService.getSupplierOutstandingReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Supplier Outstanding Report:
     *   /auditReports/supplier-outstanding/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give. Written straight to the
     * response, so the file streams out as it is built.
     */
    @PreAuthorize("@access.has('REPORTS/SUPPLIER_OUTSTANDING_REPORT/VIEW')")
    @GetMapping("/supplier-outstanding/export")
    public void exportSupplierOutstandingReport(
            @RequestParam String format,
            @ModelAttribute SupplierOutstandingReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                supplierOutstandingReportService.exportSupplierOutstandingReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
