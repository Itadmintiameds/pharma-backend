package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.IntendedUseAreaDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.IntendedUseAreaService;

import java.util.List;

@RestController
@RequestMapping("/master/intended-use-areas")
@RequiredArgsConstructor
public class IntendedUseAreaController {

    private final IntendedUseAreaService intendedUseAreaService;

    @GetMapping
    public ResponseEntity<List<IntendedUseAreaDto>> getAllIntendedUseAreas() {
        return ResponseEntity.ok(intendedUseAreaService.getAllIntendedUseAreas());
    }

    @GetMapping("/{intendedUseAreaId}")
    public ResponseEntity<IntendedUseAreaDto> getIntendedUseAreaById(@PathVariable Long intendedUseAreaId) {
        return ResponseEntity.ok(intendedUseAreaService.getIntendedUseAreaById(intendedUseAreaId));
    }

    @PostMapping
    public ResponseEntity<IntendedUseAreaDto> createIntendedUseArea(
            @RequestBody IntendedUseAreaDto intendedUseAreaDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(intendedUseAreaService.createIntendedUseArea(intendedUseAreaDto));
    }

    @PutMapping("/{intendedUseAreaId}")
    public ResponseEntity<IntendedUseAreaDto> updateIntendedUseArea(
            @PathVariable Long intendedUseAreaId,
            @RequestBody IntendedUseAreaDto intendedUseAreaDto) {

        return ResponseEntity.ok(
                intendedUseAreaService.updateIntendedUseArea(intendedUseAreaId, intendedUseAreaDto));
    }

    @PatchMapping("/{intendedUseAreaId}/status")
    public ResponseEntity<IntendedUseAreaDto> updateIntendedUseAreaStatus(
            @PathVariable Long intendedUseAreaId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                intendedUseAreaService.updateIntendedUseAreaStatus(intendedUseAreaId, request.getIsActive()));
    }
}
