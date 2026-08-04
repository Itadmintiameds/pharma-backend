package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.PurchaseSmallestUnitDto;
import tiameds.pharmabackend.service.master.PurchaseSmallestUnitService;

import java.util.List;

@RestController
@RequestMapping("/master/purchase-smallest-units")
@RequiredArgsConstructor
public class PurchaseSmallestUnitController {

    private final PurchaseSmallestUnitService purchaseSmallestUnitService;

    @GetMapping
    public ResponseEntity<List<PurchaseSmallestUnitDto>> getAllPurchaseSmallestUnits(
            @RequestParam(required = false) Long productCategoryId) {

        if (productCategoryId != null) {
            return ResponseEntity.ok(purchaseSmallestUnitService.getPurchaseSmallestUnitsByCategoryId(productCategoryId));
        }
        return ResponseEntity.ok(purchaseSmallestUnitService.getAllPurchaseSmallestUnits());
    }

    @GetMapping("/{purchaseSmallestUnitId}")
    public ResponseEntity<PurchaseSmallestUnitDto> getPurchaseSmallestUnitById(@PathVariable Long purchaseSmallestUnitId) {
        return ResponseEntity.ok(purchaseSmallestUnitService.getPurchaseSmallestUnitById(purchaseSmallestUnitId));
    }

    @PostMapping
    public ResponseEntity<PurchaseSmallestUnitDto> createPurchaseSmallestUnit(@RequestBody PurchaseSmallestUnitDto purchaseSmallestUnitDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(purchaseSmallestUnitService.createPurchaseSmallestUnit(purchaseSmallestUnitDto));
    }

    @PutMapping("/{purchaseSmallestUnitId}")
    public ResponseEntity<PurchaseSmallestUnitDto> updatePurchaseSmallestUnit(
            @PathVariable Long purchaseSmallestUnitId,
            @RequestBody PurchaseSmallestUnitDto purchaseSmallestUnitDto) {

        return ResponseEntity.ok(purchaseSmallestUnitService.updatePurchaseSmallestUnit(purchaseSmallestUnitId, purchaseSmallestUnitDto));
    }

    @PatchMapping("/{purchaseSmallestUnitId}/status")
    public ResponseEntity<PurchaseSmallestUnitDto> updatePurchaseSmallestUnitStatus(
            @PathVariable Long purchaseSmallestUnitId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                purchaseSmallestUnitService.updatePurchaseSmallestUnitStatus(purchaseSmallestUnitId, request.getIsActive()));
    }
}
