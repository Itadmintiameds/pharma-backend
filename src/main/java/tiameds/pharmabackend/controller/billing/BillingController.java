package tiameds.pharmabackend.controller.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.billing.BillingDto;
import tiameds.pharmabackend.dto.billing.BillingPaymentDto;
import tiameds.pharmabackend.dto.billing.PrescriptionUploadDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.billing.BillingService;

import java.util.List;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/create")
    public ResponseEntity<?> createBilling(
            @RequestBody BillingDto billingDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BillingDto response = billingService.createBilling(
                billingDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping(
            value = "/{billingId}/prescription",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPrescription(
            @PathVariable Long billingId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PrescriptionUploadDto response = billingService.uploadPrescription(
                billingId,
                file,
                currentUser.getUser());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/allBilling")
    public ResponseEntity<?> getAllBillings(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<BillingDto> billings =
                billingService.getAllBillings(currentUser.getUser());

        return ResponseEntity.ok(billings);
    }


    @GetMapping("/{billingId}")
    public ResponseEntity<?> getBillingById(
            @PathVariable Long billingId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BillingDto billing = billingService.getBillingById(
                billingId,
                currentUser.getUser());

        return ResponseEntity.ok(billing);
    }


    @PutMapping("/{billingId}")
    public ResponseEntity<?> updateBilling(
            @PathVariable Long billingId,
            @RequestBody BillingDto billingDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BillingDto response = billingService.updateBilling(
                billingId,
                billingDto,
                currentUser.getUser());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{billingId}/payment")
    public ResponseEntity<?> addPayment(
            @PathVariable Long billingId,
            @RequestBody BillingPaymentDto paymentDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BillingDto response = billingService.addPayment(
                billingId,
                paymentDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @DeleteMapping("/{billingId}")
    public ResponseEntity<?> deleteBilling(
            @PathVariable Long billingId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        billingService.deleteBilling(billingId, currentUser.getUser());

        return ResponseEntity.noContent().build();
    }
}
