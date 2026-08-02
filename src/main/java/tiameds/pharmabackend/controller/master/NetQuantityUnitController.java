package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.NetQuantityUnitDto;
import tiameds.pharmabackend.service.master.NetQuantityUnitService;

import java.util.List;

@RestController
@RequestMapping("/master/net-quantity-units")
@RequiredArgsConstructor
public class NetQuantityUnitController {

    private final NetQuantityUnitService netQuantityUnitService;

    @GetMapping
    public ResponseEntity<List<NetQuantityUnitDto>> getAllNetQuantityUnits(
            @RequestParam(required = false) Long productCategoryId) {

        if (productCategoryId != null) {
            return ResponseEntity.ok(
                    netQuantityUnitService.getNetQuantityUnitsByCategoryId(productCategoryId));
        }
        return ResponseEntity.ok(netQuantityUnitService.getAllNetQuantityUnits());
    }

    @GetMapping("/{netQuantityUnitId}")
    public ResponseEntity<NetQuantityUnitDto> getNetQuantityUnitById(@PathVariable Long netQuantityUnitId) {
        return ResponseEntity.ok(netQuantityUnitService.getNetQuantityUnitById(netQuantityUnitId));
    }

    @PostMapping
    public ResponseEntity<NetQuantityUnitDto> createNetQuantityUnit(
            @RequestBody NetQuantityUnitDto netQuantityUnitDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(netQuantityUnitService.createNetQuantityUnit(netQuantityUnitDto));
    }

    @PutMapping("/{netQuantityUnitId}")
    public ResponseEntity<NetQuantityUnitDto> updateNetQuantityUnit(
            @PathVariable Long netQuantityUnitId,
            @RequestBody NetQuantityUnitDto netQuantityUnitDto) {

        return ResponseEntity.ok(
                netQuantityUnitService.updateNetQuantityUnit(netQuantityUnitId, netQuantityUnitDto));
    }

    @PatchMapping("/{netQuantityUnitId}/status")
    public ResponseEntity<NetQuantityUnitDto> updateNetQuantityUnitStatus(
            @PathVariable Long netQuantityUnitId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                netQuantityUnitService.updateNetQuantityUnitStatus(netQuantityUnitId, request.getIsActive()));
    }
}
