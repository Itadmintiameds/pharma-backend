package tiameds.pharmabackend.controller.auditReports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportFilter;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.UserWiseCollectionReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditReports")
@RequiredArgsConstructor
public class UserWiseCollectionReportController {

    private final UserWiseCollectionReportService userWiseCollectionReportService;

    /**
     * User-wise Collection Report: payments collected and recorded by
     * individual users through pharmacy billing and receipt transactions.
     * Multi-select params accept repeats (?userIds=A&userIds=B)
     * or a comma list (?userIds=A,B).
     */
    @PreAuthorize("@access.has('REPORTS/USER_WISE_COLLECTION_REPORT/VIEW')")
    @GetMapping("/user-wise-collection")
    public ResponseEntity<?> getUserWiseCollectionReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) List<String> pharmacyIds,
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> paymentModes,
            @RequestParam(required = false) List<String> customerTypes,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "collectionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "totalCollection") String summarySortBy,
            @RequestParam(defaultValue = "desc") String summarySortDir,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserWiseCollectionReportFilter filter = new UserWiseCollectionReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setPharmacyIds(pharmacyIds);
        filter.setUserIds(userIds);
        filter.setPaymentModes(paymentModes);
        filter.setCustomerTypes(customerTypes);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setSummarySortBy(summarySortBy);
        filter.setSummarySortDir(summarySortDir);

        UserWiseCollectionReportResponse report =
                userWiseCollectionReportService.getUserWiseCollectionReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }
}
