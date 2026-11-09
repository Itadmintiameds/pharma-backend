package tiameds.pharmabackend.controller.financialReport.gstReport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.gstReport.GstReportService;

@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class GstReportController {

    private final GstReportService gstReportService;

    // GST Sales & Purchase Summary: sales ledger, purchase ledger and the
    // monthly Net GST. Needs fromDate and toDate (or date). Filters:
    // pharmacyIds, warehouseIds, gstin, transactionType (ALL / SALES /
    // PURCHASES), gstClassification (ALL / LOCAL / CENTRAL), gstPercentages.
    // Not paged; CSV / Excel through /financial-report/export with
    // report=gst-sales-purchase-summary.
    @GetMapping("/gst-sales-purchase-summary")
    public ResponseEntity<?> getSalesPurchaseSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(gstReportService.getSalesPurchaseSummary(filter, currentUser.getUser()));
    }


    // GSTIN dropdown: the GSTINs of the selected pharmacyIds / warehouseIds
    // (or the request's location), with the locations under each.
    @GetMapping("/gst-sales-purchase-summary/gstins")
    public ResponseEntity<?> getGstRegistrations(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(gstReportService.getGstRegistrations(filter, currentUser.getUser()));
    }
}
