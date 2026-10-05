package tiameds.pharmabackend.controller.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.warehouse.WarehouseReturnService;

import java.util.List;

@RestController
@RequestMapping("/warehouse-return")
@RequiredArgsConstructor
public class WarehouseReturnController {

    private final WarehouseReturnService warehouseReturnService;

    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createWarehouseReturn(
            @RequestBody WarehouseReturnDto warehouseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseReturnDto response = warehouseReturnService.createWarehouseReturn(
                warehouseReturnDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/VIEW')")
    @GetMapping("/getAll")
    public ResponseEntity<?> getAllWarehouseReturns(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<WarehouseReturnDto> warehouseReturns =
                warehouseReturnService.getAllWarehouseReturns(currentUser.getUser());

        return ResponseEntity.ok(warehouseReturns);
    }


    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/VIEW')")
    @GetMapping("/getById/{warehouseReturnId}")
    public ResponseEntity<?> getWarehouseReturnById(
            @PathVariable Long warehouseReturnId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseReturnDto warehouseReturn =
                warehouseReturnService.getWarehouseReturnById(warehouseReturnId, currentUser.getUser());

        return ResponseEntity.ok(warehouseReturn);
    }


    // Dispatches a draft return in full: each line's dispatchQuantity is set to its
    // returnQuantity, pharmacy stock goes out and the status moves to PENDING_RECEIPT.
    // No request body.
//    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/EDIT')")
//    @PutMapping("/{warehouseReturnId}/dispatch")
//    public ResponseEntity<?> dispatchWarehouseReturn(
//            @PathVariable Long warehouseReturnId,
//            @AuthenticationPrincipal CustomUserDetails currentUser) {
//
//        if (currentUser == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        WarehouseReturnDto response = warehouseReturnService.dispatchWarehouseReturn(
//                warehouseReturnId,
//                currentUser.getUser());
//
//        return ResponseEntity.ok(response);
//    }


    // Warehouse records what it received against a Pending Receipt return and the
    // return moves to Complete. Read from the body: totalReceivedQuantity,
    // totalNotReceivedQuantity and, for every line, warehouseReturnDetailId /
    // receivedQuantity / notReceivedQuantity.
    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/EDIT')")
    @PutMapping("/{warehouseReturnId}/receive")
    public ResponseEntity<?> receiveWarehouseReturn(
            @PathVariable Long warehouseReturnId,
            @RequestBody WarehouseReturnDto warehouseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseReturnDto response = warehouseReturnService.receiveWarehouseReturn(
                warehouseReturnId,
                warehouseReturnDto,
                currentUser.getUser());

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("@access.has('WAREHOUSE_RETURN/WAREHOUSE_RETURN/EDIT')")
    @PutMapping({"{warehouseReturnId}", "/{warehouseReturnId}/submit"})
    public ResponseEntity<?> updateWarehouseReturn(
            @PathVariable Long warehouseReturnId,
            @RequestBody WarehouseReturnDto warehouseReturnDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseReturnDto response =
                warehouseReturnService.updateWarehouseReturn(
                        warehouseReturnId,
                        warehouseReturnDto,
                        currentUser.getUser());

        return ResponseEntity.ok(response);
    }
}
