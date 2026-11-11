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
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportFilter;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.auditReports.UserWiseCollectionReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.io.IOException;
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
            @RequestParam(required = false) Boolean unpaged,

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
        filter.setUnpaged(unpaged);

        UserWiseCollectionReportResponse report =
                userWiseCollectionReportService.getUserWiseCollectionReport(filter, currentUser.getUser());

        return ResponseEntity.ok(report);
    }


    /**
     * CSV / Excel download of the User-wise Collection Report:
     *   /auditReports/user-wise-collection/export?format=xlsx&<the report's filters>
     * format is csv or xlsx. page and size are ignored: every matching receipt
     * is written, in the order sortBy / sortDir give, and the user-wise summary
     * in the order summarySortBy / summarySortDir give. Written straight to the
     * response, so the file streams out as it is built.
     */
    @GetMapping("/user-wise-collection/export")
    public void exportUserWiseCollectionReport(
            @RequestParam String format,
            @ModelAttribute UserWiseCollectionReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export = userWiseCollectionReportService.exportUserWiseCollectionReport(
                filter, format, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
