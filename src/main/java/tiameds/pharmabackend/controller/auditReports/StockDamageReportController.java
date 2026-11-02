package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockDamageReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockDamageReportController {

    private final StockDamageReportService stockDamageReportService;

    /**
     * Stock Damage Report: transaction-wise damaged stock handled through
     * Stock Return and Stock Adjustment in the period. Multi-select params
     * accept repeats (?pharmacyIds=A&pharmacyIds=B) or a comma list
     * (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_DAMAGE_REPORT/VIEW')")
    @GetMapping("/stock-damage")
    public ResponseEntity<?> getStockDamageReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) List<String> transactionTypes,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) List<String> destinationWarehouseIds,
            @RequestParam(required = false) List<Long> destinationSupplierIds,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockDamageReportFilter filter = new StockDamageReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setSupplierIds(supplierIds);
        filter.setTransactionTypes(transactionTypes);
        filter.setReferenceNumber(referenceNumber);
        filter.setDestinationWarehouseIds(destinationWarehouseIds);
        filter.setDestinationSupplierIds(destinationSupplierIds);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        StockDamageReportResponse report =
                stockDamageReportService.getStockDamageReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
