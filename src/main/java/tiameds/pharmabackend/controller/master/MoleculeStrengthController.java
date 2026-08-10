package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.ModuleStrengthDto;
import tiameds.pharmabackend.service.impl.master.MoleculeStrengthServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/moleculeStrength")
@RequiredArgsConstructor
public class MoleculeStrengthController {

    private final MoleculeStrengthServiceImpl moleculeStrengthService;

    @GetMapping
    public ResponseEntity<List<ModuleStrengthDto>> getAllMoleculeStrength() {

        List<ModuleStrengthDto> moleculeStrengths =
                moleculeStrengthService.getAllMoleculeStrength();

        return ResponseEntity.ok(moleculeStrengths);
    }
}