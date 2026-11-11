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
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.PharmacySalesReturnReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class PharmacySalesReturnReportController {

    private final PharmacySalesReturnReportService pharmacySalesReturnReportService;

    /**
     * Pharmacy Sales Return Report: Completed sales returns against earlier
     * pharmacy bills, with return value, outstanding adjustment and refund.
     * Returns the KPI cards (summary), one page of salesReturnTransactions,
     * the settlementSummary per return and the reportSummary Particulars /
     * Value table (both over every matching return, not just the page).
     * Multi-select params accept repeats (?pharmacyIds=A&pharmacyIds=B)
     * or a comma list (?pharmacyIds=A,B).
     */
    @GetMapping("/pharmacy-sales-return")
    public ResponseEntity<?> getPharmacySalesReturnReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> customerTypes,
            @RequestParam(required = false) String salesReturnNo,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String patientNumber,
            @RequestParam(required = false) String opNumber,
            @RequestParam(required = false) String ipNumber,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) List<String> returnReasons,
            @RequestParam(required = false) List<BigDecimal> gstPercentages,
            @RequestParam(required = false) BigDecimal minRefundAmount,
            @RequestParam(required = false) BigDecimal maxRefundAmount,
            @RequestParam(required = false) BigDecimal minOutstandingAdjustment,
            @RequestParam(required = false) BigDecimal maxOutstandingAdjustment,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "returnDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PharmacySalesReturnReportFilter filter = new PharmacySalesReturnReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setCustomerTypes(customerTypes);
        filter.setSalesReturnNo(salesReturnNo);
        filter.setBillNo(billNo);
        filter.setCustomerName(customerName);
        filter.setMobileNumber(mobileNumber);
        filter.setPatientNumber(patientNumber);
        filter.setOpNumber(opNumber);
        filter.setIpNumber(ipNumber);
        filter.setProductIds(productIds);
        filter.setProductName(productName);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setBatchNumber(batchNumber);
        filter.setReturnReasons(returnReasons);
        filter.setGstPercentages(gstPercentages);
        filter.setMinRefundAmount(minRefundAmount);
        filter.setMaxRefundAmount(maxRefundAmount);
        filter.setMinOutstandingAdjustment(minOutstandingAdjustment);
        filter.setMaxOutstandingAdjustment(maxOutstandingAdjustment);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        PharmacySalesReturnReportResponse report =
                pharmacySalesReturnReportService.getPharmacySalesReturnReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Pharmacy Sales Return Report:
     *   /auditReports/pharmacy-sales-return/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching line is
     * written, in the order sortBy / sortDir give, followed by the Sales
     * Return Settlement Summary and Report Summary tables. Written straight to the
     * response, so the file streams out as it is built.
     */
    @GetMapping("/pharmacy-sales-return/export")
    public void exportPharmacySalesReturnReport(
            @RequestParam String format,
            @ModelAttribute PharmacySalesReturnReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export = pharmacySalesReturnReportService.exportPharmacySalesReturnReport(
                filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
