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
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportFilter;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.ProductWiseReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class ProductWiseReportController {

    private final ProductWiseReportService productWiseReportService;

    /**
     * Product-wise Purchase and Sales Analysis Report: per product batch,
     * what was purchased from / returned to suppliers and sold to / returned
     * by customers in the period. Multi-select params accept repeats
     * (?pharmacyIds=A&pharmacyIds=B) or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/PRODUCT_WISE_REPORT/VIEW')")
    @GetMapping("/product-wise")
    public ResponseEntity<?> getProductWiseReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> warehouseIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) List<Long> supplierIds,
            @RequestParam(required = false) List<Long> productCategoryIds,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Boolean unpaged,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ProductWiseReportFilter filter = new ProductWiseReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setWarehouseIds(warehouseIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setBatchNo(batchNo);
        filter.setSupplierIds(supplierIds);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setUnpaged(unpaged);

        ProductWiseReportResponse report =
                productWiseReportService.getProductWiseReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the Product-wise Purchase and Sales Analysis Report:
     *   /auditReports/product-wise/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching product
     * batch is written, in the order sortBy / sortDir give. Written straight
     * to the response, so the file streams out as it is built.
     */
    @PreAuthorize("@access.has('REPORTS/PRODUCT_WISE_REPORT/VIEW')")
    @GetMapping("/product-wise/export")
    public void exportProductWiseReport(
            @RequestParam String format,
            @ModelAttribute ProductWiseReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export =
                productWiseReportService.exportProductWiseReport(filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
