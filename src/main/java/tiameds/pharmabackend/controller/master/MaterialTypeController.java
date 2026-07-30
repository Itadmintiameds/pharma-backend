package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MaterialTypeDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.MaterialTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/material-types")
@RequiredArgsConstructor
public class MaterialTypeController {

    private final MaterialTypeService materialTypeService;

    @GetMapping
    public ResponseEntity<List<MaterialTypeDto>> getAllMaterialTypes() {
        return ResponseEntity.ok(materialTypeService.getAllMaterialTypes());
    }

    @GetMapping("/{materialTypeId}")
    public ResponseEntity<MaterialTypeDto> getMaterialTypeById(@PathVariable Long materialTypeId) {
        return ResponseEntity.ok(materialTypeService.getMaterialTypeById(materialTypeId));
    }

    @PostMapping
    public ResponseEntity<MaterialTypeDto> createMaterialType(@RequestBody MaterialTypeDto materialTypeDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(materialTypeService.createMaterialType(materialTypeDto));
    }

    @PutMapping("/{materialTypeId}")
    public ResponseEntity<MaterialTypeDto> updateMaterialType(
            @PathVariable Long materialTypeId,
            @RequestBody MaterialTypeDto materialTypeDto) {

        return ResponseEntity.ok(materialTypeService.updateMaterialType(materialTypeId, materialTypeDto));
    }

    @PatchMapping("/{materialTypeId}/status")
    public ResponseEntity<MaterialTypeDto> updateMaterialTypeStatus(
            @PathVariable Long materialTypeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                materialTypeService.updateMaterialTypeStatus(materialTypeId, request.getIsActive()));
    }
}
