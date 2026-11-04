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
import tiameds.pharmabackend.dto.auditReports.StockMovementReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockMovementReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockMovementReportController {

    private final StockMovementReportService stockMovementReportService;

    /**
     * Stock Movement Report: every stock movement (received, issued,
     * transferred, sold, returned, adjusted) at pharmacies and warehouses
     * during the period, with a running balance. Multi-select params accept
     * repeats (?pharmacyIds=A&pharmacyIds=B) or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_MOVEMENT_REPORT/VIEW')")
    @GetMapping("/stock-movement")
    public ResponseEntity<?> getStockMovementReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<String> movementTypes,
            @RequestParam(required = false) String referenceNo,
            @RequestParam(required = false) List<String> sourceLocationIds,
            @RequestParam(required = false) List<String> destinationLocationIds,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockMovementReportFilter filter = new StockMovementReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setBatchNo(batchNo);
        filter.setMovementTypes(movementTypes);
        filter.setReferenceNo(referenceNo);
        filter.setSourceLocationIds(sourceLocationIds);
        filter.setDestinationLocationIds(destinationLocationIds);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        StockMovementReportResponse report =
                stockMovementReportService.getStockMovementReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Stock Movement Report:
     *   /auditReports/stock-movement/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give. Written straight to the
     * response, so the file streams out as it is built.
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_MOVEMENT_REPORT/VIEW')")
    @GetMapping("/stock-movement/export")
    public void exportStockMovementReport(
            @RequestParam String format,
            @ModelAttribute StockMovementReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                stockMovementReportService.exportStockMovementReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
