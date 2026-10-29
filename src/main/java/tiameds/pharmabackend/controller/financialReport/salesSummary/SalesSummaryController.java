package tiameds.pharmabackend.controller.financialReport.salesSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.salesSummary.SalesSummaryService;

@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class SalesSummaryController {

    private final SalesSummaryService salesSummaryService;

    // Cumulative sales figures (sales, discount, returns, net, IP pending)
    // across the selected pharmacies and period. See FinancialReportFilter for
    // the query parameters.
    @GetMapping("/sales-summary")
    public ResponseEntity<?> getSalesSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(salesSummaryService.getSalesSummary(filter, currentUser.getUser()));
    }
}
