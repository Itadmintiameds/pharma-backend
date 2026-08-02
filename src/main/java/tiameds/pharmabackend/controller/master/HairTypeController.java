package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.HairTypeDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.HairTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/hair-types")
@RequiredArgsConstructor
public class HairTypeController {

    private final HairTypeService hairTypeService;

    @GetMapping
    public ResponseEntity<List<HairTypeDto>> getAllHairTypes() {
        return ResponseEntity.ok(hairTypeService.getAllHairTypes());
    }

    @GetMapping("/{hairTypeId}")
    public ResponseEntity<HairTypeDto> getHairTypeById(@PathVariable Long hairTypeId) {
        return ResponseEntity.ok(hairTypeService.getHairTypeById(hairTypeId));
    }

    @PostMapping
    public ResponseEntity<HairTypeDto> createHairType(@RequestBody HairTypeDto hairTypeDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hairTypeService.createHairType(hairTypeDto));
    }

    @PutMapping("/{hairTypeId}")
    public ResponseEntity<HairTypeDto> updateHairType(
            @PathVariable Long hairTypeId,
            @RequestBody HairTypeDto hairTypeDto) {

        return ResponseEntity.ok(hairTypeService.updateHairType(hairTypeId, hairTypeDto));
    }

    @PatchMapping("/{hairTypeId}/status")
    public ResponseEntity<HairTypeDto> updateHairTypeStatus(
            @PathVariable Long hairTypeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                hairTypeService.updateHairTypeStatus(hairTypeId, request.getIsActive()));
    }
}
