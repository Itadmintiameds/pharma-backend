package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryFilter;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.CurrentStockSummaryService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class CurrentStockSummaryController {

    private final CurrentStockSummaryService currentStockSummaryService;

    /**
     * Current Stock Summary: batch-wise stock held at pharmacies and warehouses
     * as of a date. Multi-select params accept repeats (?pharmacyIds=A&pharmacyIds=B)
     * or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/CURRENT_STOCK_SUMMARY/VIEW')")
    @GetMapping("/current-stock-summary")
    public ResponseEntity<?> getCurrentStockSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryToDate,

            @RequestParam(defaultValue = "ALL") String stockStatus,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "availableStock") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CurrentStockSummaryFilter filter = new CurrentStockSummaryFilter();
        filter.setAsOfDate(asOfDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setExpiryFromDate(expiryFromDate);
        filter.setExpiryToDate(expiryToDate);
        filter.setStockStatus(stockStatus);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        CurrentStockSummaryResponse report =
                currentStockSummaryService.getCurrentStockSummary(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
