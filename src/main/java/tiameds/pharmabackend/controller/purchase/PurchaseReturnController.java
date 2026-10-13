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


    // Edits a draft return. Sending status CONFIRMED here is what takes the
    // stock out — a draft moves no inventory until this call.
    @PreAuthorize("@access.has('PURCHASE_RETURN/PURCHASE_RETURN/EDIT')")
    @PutMapping("/{purchaseReturnId}")
    public ResponseEntity<?> updatePurchaseReturn(
            @PathVariable Long purchaseReturnId,
            @RequestBody PurchaseReturnDto purchaseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseReturnDto response = purchaseReturnService.updatePurchaseReturn(
                purchaseReturnId,
                purchaseReturnDto,
                currentUser.getUser());

        return ResponseEntity.ok(response);
    }


    // Revises line quantities on a draft or confirmed return. Only
    // editReason and, per line, purchaseReturnDetailId / purchaseReturnQuantity /
    // freeReturnQuantity are read from the body.
    @PreAuthorize("@access.has('PURCHASE_RETURN/PURCHASE_RETURN/EDIT')")
    @PutMapping("/{purchaseReturnId}/edit")
    public ResponseEntity<?> editPurchaseReturn(
            @PathVariable Long purchaseReturnId,
            @RequestBody PurchaseReturnDto purchaseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseReturnDto response = purchaseReturnService.editPurchaseReturn(
                purchaseReturnId,
                purchaseReturnDto,
                currentUser.getUser());

        return ResponseEntity.ok(response);
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
