package tiameds.pharmabackend.controller.financialReport.dayClosingSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.dayClosingSummary.DayClosingSummaryService;

// Day closing summary reports. All cover one business day (date, default
// today) and share the pharmacy, patient type, payment mode and user filters;
// see FinancialReportFilter for the query parameters.
@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class DayClosingSummaryController {

    private final DayClosingSummaryService dayClosingSummaryService;

    // Bills raised on the day: total, fully paid and IP bills still pending.
    @GetMapping("/bill-count-summary")
    public ResponseEntity<?> getBillCountSummary(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getBillCountSummary(filter, currentUser.getUser()));
    }


    // Bill count and amount per patient type (IP, OP, Walk-In, Day Care)
    // plus a total.
    @GetMapping("/billing-by-patient-type")
    public ResponseEntity<?> getBillingByPatientType(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getBillingByPatientType(filter, currentUser.getUser()));
    }


    // Bill count and sales per user who raised the bills, plus a total.
    @GetMapping("/billing-by-user")
    public ResponseEntity<?> getBillingByUser(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getBillingByUser(filter, currentUser.getUser()));
    }


    // Receipt count and money collected per user who recorded the payments,
    // plus a total.
    @GetMapping("/receipt-by-user")
    public ResponseEntity<?> getReceiptByUser(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getReceiptByUser(filter, currentUser.getUser()));
    }


    // Bills raised on the day with amount received and IP pending (paged).
    @GetMapping("/current-bill-details")
    public ResponseEntity<?> getCurrentBillDetails(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getCurrentBillDetails(filter, currentUser.getUser()));
    }


    // Completed sales returns made on the day (paged).
    @GetMapping("/sales-return-details")
    public ResponseEntity<?> getSalesReturnDetails(
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(dayClosingSummaryService.getSalesReturnDetails(filter, currentUser.getUser()));
    }
}
