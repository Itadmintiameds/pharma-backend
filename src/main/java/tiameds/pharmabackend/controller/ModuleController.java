package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.ModuleDto;
import tiameds.pharmabackend.dto.ModuleSummaryDto;
import tiameds.pharmabackend.service.ModuleService;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping
    public ResponseEntity<List<ModuleDto>> getModulesWithFeatures() {
        return ResponseEntity.ok(moduleService.getModulesWithFeatures());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ModuleSummaryDto>> getAllModules() {
        return ResponseEntity.ok(moduleService.getAllModules());
    }

    @GetMapping("/{moduleId}/features")
    public ResponseEntity<ModuleDto> getModuleWithFeatures(
            @PathVariable Long moduleId) {

        return ResponseEntity.ok(moduleService.getModuleWithFeatures(moduleId));
    }
}
