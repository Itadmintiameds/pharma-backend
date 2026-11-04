package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.SupplierOutstandingReportService;

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

        SupplierOutstandingReportResponse report =
                supplierOutstandingReportService.getSupplierOutstandingReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
