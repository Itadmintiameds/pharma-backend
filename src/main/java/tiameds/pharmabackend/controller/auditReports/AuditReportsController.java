package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportFilter;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.AuditReportsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class AuditReportsController {

    private final AuditReportsService auditReportsService;

    /**
     * Inventory Sales Report: product-wise sales and sales returns for the
     * period. Multi-select params accept repeats (?pharmacyIds=A&pharmacyIds=B)
     * or a comma list (?pharmacyIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/INVENTORY_SALES_REPORT/VIEW')")
    @GetMapping("/inventory-sales")
    public ResponseEntity<?> getInventorySalesReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> productNames,
            @RequestParam(required = false) List<Long> productCategoryIds,
            @RequestParam(required = false) List<String> hsnCodes,
            @RequestParam(required = false) String hsnSearch,
            @RequestParam(required = false) List<BigDecimal> gstPercentages,
            @RequestParam(required = false) List<Long> doctorIds,
            @RequestParam(required = false) List<String> patientTypes,
            @RequestParam(required = false) List<String> drugSchedules,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "netSales") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        InventorySalesReportFilter filter = new InventorySalesReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setProductIds(productIds);
        filter.setProductNames(productNames);
        filter.setProductCategoryIds(productCategoryIds);
        filter.setHsnCodes(hsnCodes);
        filter.setHsnSearch(hsnSearch);
        filter.setGstPercentages(gstPercentages);
        filter.setDoctorIds(doctorIds);
        filter.setPatientTypes(patientTypes);
        filter.setDrugSchedules(drugSchedules);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        InventorySalesReportResponse report =
                auditReportsService.getInventorySalesReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
