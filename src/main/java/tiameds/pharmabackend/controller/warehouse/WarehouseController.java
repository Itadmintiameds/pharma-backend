package tiameds.pharmabackend.controller.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.warehouse.WarehouseDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.warehouse.WarehouseService;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/create")
    public ResponseEntity<?> createWarehouse(
            @RequestBody WarehouseDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseDto response = warehouseService.createWarehouse(dto, currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/update/{warehouseId}")
    public ResponseEntity<?> updateWarehouse(
            @PathVariable Long warehouseId,
            @RequestBody WarehouseDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseDto response = warehouseService.updateWarehouse(warehouseId, dto, currentUser.getUser());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<WarehouseDto> getWarehouse(
            @PathVariable Long warehouseId) {

        return ResponseEntity.ok(warehouseService.getWarehouseById(warehouseId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<WarehouseDto>> listWarehouses(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(warehouseService.getWarehousesForUser(currentUser.getUser()));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<WarehouseDto>> getWarehousesByOrganizationId(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(warehouseService.getWarehousesByOrganizationId(organizationId, currentUser.getUser()));
    }

    @DeleteMapping("/{warehouseId}")
    public ResponseEntity<?> deleteWarehouse(
            @PathVariable Long warehouseId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        warehouseService.deleteWarehouse(warehouseId);

        return ResponseEntity.ok("Warehouse deleted successfully.");
    }
}
