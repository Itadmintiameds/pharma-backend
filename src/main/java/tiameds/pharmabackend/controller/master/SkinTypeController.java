package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.SkinTypeDto;
import tiameds.pharmabackend.service.master.SkinTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/skin-types")
@RequiredArgsConstructor
public class SkinTypeController {

    private final SkinTypeService skinTypeService;

    @GetMapping
    public ResponseEntity<List<SkinTypeDto>> getAllSkinTypes() {
        return ResponseEntity.ok(skinTypeService.getAllSkinTypes());
    }

    @GetMapping("/{skinTypeId}")
    public ResponseEntity<SkinTypeDto> getSkinTypeById(@PathVariable Long skinTypeId) {
        return ResponseEntity.ok(skinTypeService.getSkinTypeById(skinTypeId));
    }

    @PostMapping
    public ResponseEntity<SkinTypeDto> createSkinType(@RequestBody SkinTypeDto skinTypeDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(skinTypeService.createSkinType(skinTypeDto));
    }

    @PutMapping("/{skinTypeId}")
    public ResponseEntity<SkinTypeDto> updateSkinType(
            @PathVariable Long skinTypeId,
            @RequestBody SkinTypeDto skinTypeDto) {

        return ResponseEntity.ok(skinTypeService.updateSkinType(skinTypeId, skinTypeDto));
    }

    @PatchMapping("/{skinTypeId}/status")
    public ResponseEntity<SkinTypeDto> updateSkinTypeStatus(
            @PathVariable Long skinTypeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                skinTypeService.updateSkinTypeStatus(skinTypeId, request.getIsActive()));
    }
}
