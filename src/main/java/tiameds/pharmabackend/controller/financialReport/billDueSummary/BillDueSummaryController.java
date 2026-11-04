package tiameds.pharmabackend.controller.financialReport.billDueSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.billDueSummary.BillDueSummaryService;

@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class BillDueSummaryController {

    private final BillDueSummaryService billDueSummaryService;

    // Amount received and pending on IP bills. See FinancialReportFilter for
    // the query parameters; pendingStatus picks PENDING, FULLY_PAID or ALL.
    @GetMapping("/bill-due-summary")
    public ResponseEntity<?> getBillDueSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(billDueSummaryService.getBillDueSummary(filter, currentUser.getUser()));
    }
}
