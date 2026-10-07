package tiameds.pharmabackend.controller.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.billing.SalesReturnDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.billing.SalesReturnService;

import java.util.List;

@RestController
@RequestMapping("/sales-return")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    // Reads billingId, optional salesReturnDate, and per line productId /
    // batchId / salesReturnQuantity / salesReturnReason. Amounts are priced
    // off the bill by the server.
    @PreAuthorize("@access.has('SALES_RETURN/SALES_RETURN/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createSalesReturn(
            @RequestBody SalesReturnDto salesReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SalesReturnDto response = salesReturnService.createSalesReturn(
                salesReturnDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("@access.has('SALES_RETURN/SALES_RETURN/VIEW')")
    @GetMapping("/allSalesReturn")
    public ResponseEntity<?> getAllSalesReturns(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SalesReturnDto> salesReturns =
                salesReturnService.getAllSalesReturns(currentUser.getUser());

        return ResponseEntity.ok(salesReturns);
    }


    // KPI cards for the sales return screen: number of returns and total
    // amount refunded, for the selected pharmacy.
    @PreAuthorize("@access.has('SALES_RETURN/SALES_RETURN/VIEW')")
    @GetMapping("/kpi")
    public ResponseEntity<?> getSalesReturnKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(salesReturnService.getSalesReturnKpis(currentUser.getUser()));
    }


    @PreAuthorize("@access.has('SALES_RETURN/SALES_RETURN/VIEW')")
    @GetMapping("/{salesReturnId}")
    public ResponseEntity<?> getSalesReturnById(
            @PathVariable Long salesReturnId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SalesReturnDto salesReturn = salesReturnService.getSalesReturnById(
                salesReturnId,
                currentUser.getUser());

        return ResponseEntity.ok(salesReturn);
    }
}
