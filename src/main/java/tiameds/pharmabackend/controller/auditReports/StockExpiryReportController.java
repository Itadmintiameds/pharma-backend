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
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockExpiryReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockExpiryReportController {

    private final StockExpiryReportService stockExpiryReportService;

    /**
     * Stock Expiry Report: batch-wise stock on hand at pharmacies and
     * warehouses with its expiry status. Multi-select params accept repeats
     * (?pharmacyIds=A&pharmacyIds=B) or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_EXPIRY_REPORT/VIEW')")
    @GetMapping("/stock-expiry")
    public ResponseEntity<?> getStockExpiryReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<Long> supplierIds,

            @RequestParam(defaultValue = "ALL") String expiryStatus,
            @RequestParam(required = false) Integer expiryWithinMonths,
            @RequestParam(required = false) Integer expiryWithinDays,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth expiryFromMonth,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth expiryToMonth,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "expiryDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockExpiryReportFilter filter = new StockExpiryReportFilter();
        filter.setAsOfDate(asOfDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setSupplierIds(supplierIds);
        filter.setExpiryStatus(expiryStatus);
        filter.setExpiryWithinMonths(expiryWithinMonths);
        filter.setExpiryWithinDays(expiryWithinDays);
        filter.setExpiryFromMonth(expiryFromMonth);
        filter.setExpiryToMonth(expiryToMonth);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        StockExpiryReportResponse report =
                stockExpiryReportService.getStockExpiryReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Stock Expiry Report:
     *   /auditReports/stock-expiry/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give. Written straight to the
     * response, so the file streams out as it is built.
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_EXPIRY_REPORT/VIEW')")
    @GetMapping("/stock-expiry/export")
    public void exportStockExpiryReport(
            @RequestParam String format,
            @ModelAttribute StockExpiryReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                stockExpiryReportService.exportStockExpiryReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
