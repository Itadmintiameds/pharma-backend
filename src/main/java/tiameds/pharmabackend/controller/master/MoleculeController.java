package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.MoleculeDto;
import tiameds.pharmabackend.service.master.MoleculeService;

import java.util.List;

@RestController
@RequestMapping("/master/molecules")
@RequiredArgsConstructor
public class MoleculeController {

    private final MoleculeService moleculeService;

    @GetMapping
    public ResponseEntity<List<MoleculeDto>> getAllMolecules() {
        return ResponseEntity.ok(moleculeService.getAllMolecules());
    }

    @GetMapping("/{moleculeId}")
    public ResponseEntity<MoleculeDto> getMoleculeById(@PathVariable Long moleculeId) {
        return ResponseEntity.ok(moleculeService.getMoleculeById(moleculeId));
    }

    @PostMapping
    public ResponseEntity<MoleculeDto> createMolecule(@RequestBody MoleculeDto moleculeDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(moleculeService.createMolecule(moleculeDto));
    }

    @PutMapping("/{moleculeId}")
    public ResponseEntity<MoleculeDto> updateMolecule(
            @PathVariable Long moleculeId,
            @RequestBody MoleculeDto moleculeDto) {

        return ResponseEntity.ok(moleculeService.updateMolecule(moleculeId, moleculeDto));
    }

    @PatchMapping("/{moleculeId}/status")
    public ResponseEntity<MoleculeDto> updateMoleculeStatus(
            @PathVariable Long moleculeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                moleculeService.updateMoleculeStatus(moleculeId, request.getIsActive()));
    }
}
