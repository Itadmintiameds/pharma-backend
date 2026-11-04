package tiameds.pharmabackend.controller.financialReport.pharmacyReceipts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.pharmacyReceipts.PharmacyReceiptsService;

// Pharmacy receipts reports. All share the same filters; see
// FinancialReportFilter for the query parameters.
@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class PharmacyReceiptsController {

    private final PharmacyReceiptsService pharmacyReceiptsService;

    // Cumulative sales figures (sales, discount, returns, net, IP pending)
    // across the selected pharmacies and period.
    @GetMapping("/sales-summary")
    public ResponseEntity<?> getSalesSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(pharmacyReceiptsService.getSalesSummary(filter, currentUser.getUser()));
    }


    // Amount received per payment mode (card, cash, UPI) plus a total.
    @GetMapping("/payment-mode-summary")
    public ResponseEntity<?> getPaymentModeSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(pharmacyReceiptsService.getPaymentModeSummary(filter, currentUser.getUser()));
    }


    // Money received in the period, split by whether its bill was raised in
    // the period (current) or before it (previous). Needs date, or both
    // fromDate and toDate.
    @GetMapping("/receipt-details")
    public ResponseEntity<?> getReceiptDetails(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(pharmacyReceiptsService.getReceiptDetails(filter, currentUser.getUser()));
    }
}
