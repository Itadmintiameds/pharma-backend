package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.SupplierPaymentReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class SupplierPaymentReportController {

    private final SupplierPaymentReportService supplierPaymentReportService;

    /**
     * Supplier Payment Report: transaction-wise, supplier-wise and purchase
     * invoice-wise payments made to suppliers against credit purchase
     * invoices. Multi-select params accept repeats
     * (?paymentModes=CASH&paymentModes=UPI) or a comma list (?paymentModes=CASH,UPI).
     */
    @PreAuthorize("@access.has('REPORTS/SUPPLIER_PAYMENT_REPORT/VIEW')")
    @GetMapping("/supplier-payment")
    public ResponseEntity<?> getSupplierPaymentReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) String paymentNumber,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String grnNumber,
            @RequestParam(required = false) List<String> paymentModes,
            @RequestParam(required = false) BigDecimal minPaymentAmount,
            @RequestParam(required = false) BigDecimal maxPaymentAmount,
            @RequestParam(required = false) String referenceNumber,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentReportFilter filter = new SupplierPaymentReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setSupplierIds(supplierIds);
        filter.setPaymentNumber(paymentNumber);
        filter.setInvoiceNumber(invoiceNumber);
        filter.setGrnNumber(grnNumber);
        filter.setPaymentModes(paymentModes);
        filter.setMinPaymentAmount(minPaymentAmount);
        filter.setMaxPaymentAmount(maxPaymentAmount);
        filter.setReferenceNumber(referenceNumber);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        SupplierPaymentReportResponse report =
                supplierPaymentReportService.getSupplierPaymentReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
