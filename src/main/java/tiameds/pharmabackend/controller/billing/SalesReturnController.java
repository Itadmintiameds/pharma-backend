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
}
