package tiameds.pharmabackend.controller.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.purchase.PurchaseReturnService;

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
}
