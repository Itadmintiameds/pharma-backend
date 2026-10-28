package tiameds.pharmabackend.controller.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.supplier.SupplierDuplicateCheckResponse;
import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.supplier.SupplierMasterService;

import java.util.List;

@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
public class SupplierMasterController {

    private final SupplierMasterService supplierMasterService;

    @PreAuthorize("@access.has('SUPPLIER_MASTER/SUPPLIER_MASTER/CREATE')")
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

    @PreAuthorize("@access.has('SUPPLIER_MASTER/SUPPLIER_MASTER/VIEW')")
    @GetMapping("/allSupplier")
    public ResponseEntity<?> getAllSuppliers(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SupplierMasterDto> suppliers =
                supplierMasterService.getAllSuppliers(currentUser.getUser());

        return ResponseEntity.ok(suppliers);
    }


    @PreAuthorize("@access.has('SUPPLIER_MASTER/SUPPLIER_MASTER/VIEW')")
    @GetMapping("/getById/{supplierId}")
    public ResponseEntity<?> getSupplierById(
            @PathVariable Long supplierId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierMasterDto supplier =
                supplierMasterService.getSupplierById(
                        supplierId,
                        currentUser.getUser());

        return ResponseEntity.ok(supplier);
    }

    @PreAuthorize("@access.has('SUPPLIER_MASTER/SUPPLIER_MASTER/EDIT')")
    @PutMapping("/update/{supplierId}")
    public ResponseEntity<?> updateSupplier(
            @PathVariable Long supplierId,
            @RequestBody SupplierMasterDto supplierDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierMasterDto response =
                supplierMasterService.updateSupplier(
                        supplierId,
                        supplierDto,
                        currentUser.getUser());

        return ResponseEntity.ok(response);
    }

    // Checks GST / DL / PAN / FSSAI numbers against the other suppliers of the
    // current warehouse or pharmacy before saving. Send supplierId when editing
    // so the supplier is not reported as a duplicate of itself.
    @PreAuthorize("@access.hasAny('SUPPLIER_MASTER/SUPPLIER_MASTER/CREATE', 'SUPPLIER_MASTER/SUPPLIER_MASTER/EDIT')")
    @PostMapping("/checkDuplicate")
    public ResponseEntity<?> checkDuplicateSupplier(
            @RequestBody SupplierMasterDto supplierDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SupplierDuplicateCheckResponse response =
                supplierMasterService.checkDuplicateSupplier(
                        supplierDto,
                        currentUser.getUser());

        return ResponseEntity.ok(response);
    }
}