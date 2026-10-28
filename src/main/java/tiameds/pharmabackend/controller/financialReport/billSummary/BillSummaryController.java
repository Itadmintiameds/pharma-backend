package tiameds.pharmabackend.controller.financialReport.billSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.billSummary.BillSummaryService;

@RestController
@RequestMapping("/financial-report/bill-summary")
@RequiredArgsConstructor
public class BillSummaryController {

    private final BillSummaryService billSummaryService;

    // Bill-wise sales for the pharmacy in the X-Pharmacy-Id header, with KPI
    // totals over all matching bills. See FinancialReportFilter for the query
    // parameters (date / range, searches, referred-by, page, size).
    @PreAuthorize("@access.has('FINANCIAL_REPORTS/BILL_SUMMARY/VIEW')")
    @GetMapping
    public ResponseEntity<?> getBillSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(billSummaryService.getBillSummary(filter, currentUser.getUser()));
    }
}
