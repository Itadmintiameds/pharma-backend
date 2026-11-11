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
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockAdjustmentReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockAdjustmentReportController {

    private final StockAdjustmentReportService stockAdjustmentReportService;

    /**
     * Stock Adjustment Report: transaction-wise inventory quantities increased
     * or decreased through Stock Adjustment in the period. Returns the KPI
     * cards (summary), one page of adjustmentTransactions, and the
     * adjustmentTypeSummary / adjustmentReasonSummary tables (both over every
     * matching row, not just the page). Multi-select params
     * accept repeats (?pharmacyIds=A&pharmacyIds=B) or a comma list
     * (?pharmacyIds=A,B).
     */
    @GetMapping("/stock-adjustment")
    public ResponseEntity<?> getStockAdjustmentReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) String adjustmentType,
            @RequestParam(required = false) List<String> adjustmentReasons,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) List<String> adjustedByIds,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "adjustmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockAdjustmentReportFilter filter = new StockAdjustmentReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setSupplierIds(supplierIds);
        filter.setAdjustmentType(adjustmentType);
        filter.setAdjustmentReasons(adjustmentReasons);
        filter.setReferenceNumber(referenceNumber);
        filter.setAdjustedByIds(adjustedByIds);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        StockAdjustmentReportResponse report =
                stockAdjustmentReportService.getStockAdjustmentReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Stock Adjustment Report:
     *   /auditReports/stock-adjustment/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give, followed by the Adjustment
     * Type Summary and Adjustment Reason Summary tables. Written straight to the
     * response, so the file streams out as it is built.
     */
    @GetMapping("/stock-adjustment/export")
    public void exportStockAdjustmentReport(
            @RequestParam String format,
            @ModelAttribute StockAdjustmentReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                stockAdjustmentReportService.exportStockAdjustmentReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }

    /**
     * Options for the Adjustment Reason multi-select: distinct reasons recorded
     * at the given pharmacies (every accessible pharmacy when none sent).
     */
    @GetMapping("/stock-adjustment/reasons")
    public ResponseEntity<?> getAdjustmentReasons(
            @RequestParam(required = false) List<String> pharmacyIds,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                stockAdjustmentReportService.getAdjustmentReasons(pharmacyIds, currentUser.getUser()));
    }
}
