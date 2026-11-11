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
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.SupplierPurchaseReturnReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class SupplierPurchaseReturnReportController {

    private final SupplierPurchaseReturnReportService supplierPurchaseReturnReportService;

    /**
     * Supplier Purchase Return Report: transaction-wise products returned to
     * suppliers through the Purchase Return workflow in the period. Returns the
     * KPI cards (summary, which also carries the table's Total row), one page
     * of purchaseReturnDetails, and the purchaseReturnSummary Metric / Value
     * table (over every matching row, not just the page). Multi-select
     * params accept repeats (?supplierIds=1&supplierIds=2) or a comma list
     * (?supplierIds=1,2). expiryFrom / expiryTo are months as MM/yyyy.
     */
    @GetMapping("/supplier-purchase-return")
    public ResponseEntity<?> getSupplierPurchaseReturnReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) String purchaseReturnNo,
            @RequestParam(required = false) String purchaseInvoiceNo,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> brandNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String expiryFrom,
            @RequestParam(required = false) String expiryTo,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<BigDecimal> gstPercentages,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "returnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPurchaseReturnReportFilter filter = new SupplierPurchaseReturnReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setSupplierIds(supplierIds);
        filter.setPurchaseReturnNo(purchaseReturnNo);
        filter.setPurchaseInvoiceNo(purchaseInvoiceNo);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBrandNames(brandNames);
        filter.setBatchNo(batchNo);
        filter.setExpiryFrom(expiryFrom);
        filter.setExpiryTo(expiryTo);
        filter.setStatuses(statuses);
        filter.setGstPercentages(gstPercentages);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        SupplierPurchaseReturnReportResponse report =
                supplierPurchaseReturnReportService.getSupplierPurchaseReturnReport(
                        filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Supplier Purchase Return Report:
     *   /auditReports/supplier-purchase-return/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching row is
     * written, in the order sortBy / sortDir give, followed by the Supplier
     * Purchase Return Summary table. Written straight to the
     * response, so the file streams out as it is built.
     */
    @GetMapping("/supplier-purchase-return/export")
    public void exportSupplierPurchaseReturnReport(
            @RequestParam String format,
            @ModelAttribute SupplierPurchaseReturnReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export = supplierPurchaseReturnReportService.exportSupplierPurchaseReturnReport(
                filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
