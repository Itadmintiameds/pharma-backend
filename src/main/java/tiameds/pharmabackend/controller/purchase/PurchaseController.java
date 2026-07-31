package tiameds.pharmabackend.controller.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.purchase.PurchaseService;

import java.util.List;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/create")
    public ResponseEntity<?> createPurchase(
            @RequestBody PurchaseDto purchaseDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PurchaseDto response = purchaseService.createPurchase(
                purchaseDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/allPurchase")
    public ResponseEntity<?> getAllPurchases(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<PurchaseDto> purchases =
                purchaseService.getAllPurchases(currentUser.getUser());

        return ResponseEntity.ok(purchases);
    }
}