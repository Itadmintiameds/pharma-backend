package tiameds.pharmabackend.controller.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.warehouse.WarehouseDistributionService;

import java.util.Map;

@RestController
@RequestMapping("/warehouse/distribution")
@RequiredArgsConstructor
public class WarehouseDistributionController {

    private final WarehouseDistributionService distributionService;

    // Next allocation number to display on a blank create form (preview only)
    @GetMapping("/next-allocation-no")
    public ResponseEntity<?> nextAllocationNo(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of("allocationNo", distributionService.peekNextAllocationNo()));
    }

    // Create an allocation (DISTRIBUTION_CREATED)
    @PostMapping("/create")
    public ResponseEntity<?> createAllocation(
            @RequestBody WarehouseDistributionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseDistributionResponse response =
                distributionService.createAllocation(request, currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Dispatch: source stock leaves (PRODUCTS_DISPATCHED)
    @PostMapping("/{distributionId}/dispatch")
    public ResponseEntity<?> dispatch(
            @PathVariable Long distributionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        distributionService.dispatch(distributionId, currentUser.getUser());

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }

    // Receive: destination stock arrives (STOCK_RECEIVED)
    @PostMapping("/{distributionId}/receive")
    public ResponseEntity<?> receive(
            @PathVariable Long distributionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        distributionService.receive(distributionId, currentUser.getUser());

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }

    // Read one distribution with its lines and current status
    @GetMapping("/{distributionId}")
    public ResponseEntity<WarehouseDistributionResponse> getById(
            @PathVariable Long distributionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }
}
