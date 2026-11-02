package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockAdjustmentReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockAdjustmentReportController {

    private final StockAdjustmentReportService stockAdjustmentReportService;

    /**
     * Stock Adjustment Report: transaction-wise inventory quantities increased
     * or decreased through Stock Adjustment in the period. Multi-select params
     * accept repeats (?pharmacyIds=A&pharmacyIds=B) or a comma list
     * (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_ADJUSTMENT_REPORT/VIEW')")
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

        StockAdjustmentReportResponse report =
                stockAdjustmentReportService.getStockAdjustmentReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }

    /**
     * Options for the Adjustment Reason multi-select: distinct reasons recorded
     * at the given pharmacies (every accessible pharmacy when none sent).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_ADJUSTMENT_REPORT/VIEW')")
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
