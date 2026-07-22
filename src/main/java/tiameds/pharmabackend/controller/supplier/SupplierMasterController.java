package tiameds.pharmabackend.controller.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.supplier.SupplierMasterService;

@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
public class SupplierMasterController {

    private final SupplierMasterService supplierMasterService;

    @PostMapping("/create")
    public ResponseEntity<?> createSupplier(
            @RequestBody SupplierMasterDto supplierDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierMasterDto response =
                supplierMasterService.createSupplier(
                        supplierDto,
                        currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}