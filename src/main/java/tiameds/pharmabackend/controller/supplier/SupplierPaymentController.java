package tiameds.pharmabackend.controller.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.supplier.SupplierPaymentService;

import java.util.List;

@RestController
@RequestMapping("/supplier-payments")
@RequiredArgsConstructor
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;

    // Records a payment against a purchase of the caller's current warehouse or
    // pharmacy and marks that purchase as PAID.
    @PreAuthorize("@access.has('PURCHASE/PURCHASE/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestBody SupplierPaymentDto paymentDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto response = supplierPaymentService.createPayment(
                paymentDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("@access.has('PURCHASE/PURCHASE/VIEW')")
    @GetMapping("/allSupplierPayments")
    public ResponseEntity<?> getAllPayments(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SupplierPaymentDto> payments =
                supplierPaymentService.getAllPayments(currentUser.getUser());

        return ResponseEntity.ok(payments);
    }


    // KPI cards for the supplier payment list: total supplier payable, overdue
    // payable and supplier credit, for the current warehouse or pharmacy.
    @PreAuthorize("@access.has('PURCHASE/PURCHASE/VIEW')")
    @GetMapping("/kpi")
    public ResponseEntity<?> getSupplierPaymentKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(supplierPaymentService.getSupplierPaymentKpis(currentUser.getUser()));
    }


    @PreAuthorize("@access.has('PURCHASE/PURCHASE/VIEW')")
    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<?> getPaymentsByPurchase(
            @PathVariable Long purchaseId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SupplierPaymentDto> payments = supplierPaymentService.getPaymentsByPurchase(
                purchaseId,
                currentUser.getUser());

        return ResponseEntity.ok(payments);
    }


    @PreAuthorize("@access.has('PURCHASE/PURCHASE/VIEW')")
    @GetMapping("/getById/{supplierPaymentId}")
    public ResponseEntity<?> getPaymentById(
            @PathVariable Long supplierPaymentId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierPaymentDto payment = supplierPaymentService.getPaymentById(
                supplierPaymentId,
                currentUser.getUser());

        return ResponseEntity.ok(payment);
    }
}
