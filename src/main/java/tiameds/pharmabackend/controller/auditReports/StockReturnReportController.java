package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.StockReturnReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class StockReturnReportController {

    private final StockReturnReportService stockReturnReportService;

    /**
     * Stock Return Report: transaction-wise view of products returned from
     * pharmacies and warehouses in the period. Multi-select params accept
     * repeats (?pharmacyIds=A&pharmacyIds=B) or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/STOCK_RETURN_REPORT/VIEW')")
    @GetMapping("/stock-return")
    public ResponseEntity<?> getStockReturnReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) List<String> returnTypes,
            @RequestParam(required = false) List<String> returnReasons,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) List<String> sourceLocationTypes,
            @RequestParam(required = false) List<String> destinationTypes,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "returnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockReturnReportFilter filter = new StockReturnReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setSupplierIds(supplierIds);
        filter.setReturnTypes(returnTypes);
        filter.setReturnReasons(returnReasons);
        filter.setReferenceNumber(referenceNumber);
        filter.setSourceLocationTypes(sourceLocationTypes);
        filter.setDestinationTypes(destinationTypes);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        StockReturnReportResponse report =
                stockReturnReportService.getStockReturnReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
