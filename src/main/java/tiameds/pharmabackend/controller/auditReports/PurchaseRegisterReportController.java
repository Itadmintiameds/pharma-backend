package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportFilter;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.PurchaseRegisterReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class PurchaseRegisterReportController {

    private final PurchaseRegisterReportService purchaseRegisterReportService;

    /**
     * Purchase Register Report: transaction-wise medicines purchased through
     * supplier purchase invoices in the period. Multi-select params accept
     * repeats (?supplierIds=1&supplierIds=2) or a comma list (?supplierIds=1,2).
     * expiryFrom / expiryTo are months as MM/yyyy.
     */
    @PreAuthorize("@access.has('REPORTS/PURCHASE_REGISTER_REPORT/VIEW')")
    @GetMapping("/purchase-register")
    public ResponseEntity<?> getPurchaseRegisterReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String expiryFrom,
            @RequestParam(required = false) String expiryTo,
            @RequestParam(required = false) List<BigDecimal> gstPercentages,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "purchaseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseRegisterReportFilter filter = new PurchaseRegisterReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setSupplierIds(supplierIds);
        filter.setInvoiceNumber(invoiceNumber);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setExpiryFrom(expiryFrom);
        filter.setExpiryTo(expiryTo);
        filter.setGstPercentages(gstPercentages);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        PurchaseRegisterReportResponse report =
                purchaseRegisterReportService.getPurchaseRegisterReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
