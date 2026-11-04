package tiameds.pharmabackend.controller.financialReport.billSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.billSummary.BillSummaryService;

// All reports read the pharmacy from the X-Pharmacy-Id header. See
// FinancialReportFilter for the query parameters (date / range, searches,
// multi-selects, page, size).
@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class BillSummaryController {

    private final BillSummaryService billSummaryService;

    // Bill-wise sales, with KPI totals over all matching bills.
    @GetMapping("/bill-summary")
    public ResponseEntity<?> getBillSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(billSummaryService.getBillSummary(filter, currentUser.getUser()));
    }


    // Bill detailed sales: one row per product line on each bill, so a bill
    // with two products comes back as two rows.
    @GetMapping("/detailed-sale-report")
    public ResponseEntity<?> getBillDetailedSales(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(billSummaryService.getBillDetailedSales(filter, currentUser.getUser()));
    }
}
