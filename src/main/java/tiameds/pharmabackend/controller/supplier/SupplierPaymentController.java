package tiameds.pharmabackend.controller.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.supplier.SupplierPaymentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/supplier-payments")
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestBody SupplierPaymentDto paymentDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto response =
                supplierPaymentService.createPayment(paymentDto, currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<?> getPaymentsByPurchase(
            @PathVariable Long purchaseId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SupplierPaymentDto> response =
                supplierPaymentService.getPaymentsByPurchase(purchaseId, currentUser.getUser());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{supplierPaymentId}")
    public ResponseEntity<?> getPaymentById(
            @PathVariable Long supplierPaymentId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto response =
                supplierPaymentService.getPaymentById(supplierPaymentId, currentUser.getUser());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{supplierPaymentId}")
    public ResponseEntity<?> updatePayment(
            @PathVariable Long supplierPaymentId,
            @RequestBody SupplierPaymentDto paymentDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto response =
                supplierPaymentService.updatePayment(
                        supplierPaymentId, paymentDto, currentUser.getUser());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{supplierPaymentId}/status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Long supplierPaymentId,
            @RequestBody MasterStatusDto request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto response =
                supplierPaymentService.updatePaymentStatus(
                        supplierPaymentId, request.getIsActive(), currentUser.getUser());

        return ResponseEntity.ok(response);
    }
}
