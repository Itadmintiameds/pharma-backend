package tiameds.pharmabackend.controller.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.purchase.PurchaseReturnService;

import java.util.List;

@RestController
@RequestMapping("/purchase-return")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @PreAuthorize("@access.has('PURCHASE_RETURN/PURCHASE_RETURN/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createPurchaseReturn(
            @RequestBody PurchaseReturnDto purchaseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseReturnDto response = purchaseReturnService.createPurchaseReturn(
                purchaseReturnDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("@access.has('PURCHASE_RETURN/PURCHASE_RETURN/VIEW')")
    @GetMapping("/allPurchaseReturn")
    public ResponseEntity<?> getAllPurchaseReturns(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<PurchaseReturnDto> purchaseReturns =
                purchaseReturnService.getAllPurchaseReturns(currentUser.getUser());

        return ResponseEntity.ok(purchaseReturns);
    }


    @PreAuthorize("@access.has('PURCHASE_RETURN/PURCHASE_RETURN/VIEW')")
    @GetMapping("/{purchaseReturnId}")
    public ResponseEntity<?> getPurchaseReturnById(
            @PathVariable Long purchaseReturnId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseReturnDto purchaseReturn =
                purchaseReturnService.getPurchaseReturnById(purchaseReturnId, currentUser.getUser());

        return ResponseEntity.ok(purchaseReturn);
    }
}
