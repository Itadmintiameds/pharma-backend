package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.TherapeuticCategoryDto;
import tiameds.pharmabackend.dto.master.TherapeuticSubcategoryDto;
import tiameds.pharmabackend.service.master.TherapeuticCategoryService;
import tiameds.pharmabackend.service.master.TherapeuticSubcategoryService;

import java.util.List;

@RestController
@RequestMapping("/master/therapeutic-categories")
@RequiredArgsConstructor
public class TherapeuticCategoryController {

    private final TherapeuticCategoryService therapeuticCategoryService;
    private final TherapeuticSubcategoryService therapeuticSubcategoryService;

    @GetMapping
    public ResponseEntity<List<TherapeuticCategoryDto>> getAllTherapeuticCategories() {
        return ResponseEntity.ok(therapeuticCategoryService.getAllTherapeuticCategories());
    }

    @GetMapping("/{therapeuticCategoryId}")
    public ResponseEntity<TherapeuticCategoryDto> getTherapeuticCategoryById(
            @PathVariable Long therapeuticCategoryId) {

        return ResponseEntity.ok(
                therapeuticCategoryService.getTherapeuticCategoryById(therapeuticCategoryId));
    }

    @GetMapping("/{therapeuticCategoryId}/subcategories")
    public ResponseEntity<List<TherapeuticSubcategoryDto>> getSubcategoriesByCategoryId(
            @PathVariable Long therapeuticCategoryId) {

        return ResponseEntity.ok(
                therapeuticSubcategoryService.getSubcategoriesByCategoryId(therapeuticCategoryId));
    }

    @PostMapping
    public ResponseEntity<TherapeuticCategoryDto> createTherapeuticCategory(
            @RequestBody TherapeuticCategoryDto therapeuticCategoryDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(therapeuticCategoryService.createTherapeuticCategory(therapeuticCategoryDto));
    }

    @PutMapping("/{therapeuticCategoryId}")
    public ResponseEntity<TherapeuticCategoryDto> updateTherapeuticCategory(
            @PathVariable Long therapeuticCategoryId,
            @RequestBody TherapeuticCategoryDto therapeuticCategoryDto) {

        return ResponseEntity.ok(
                therapeuticCategoryService.updateTherapeuticCategory(
                        therapeuticCategoryId, therapeuticCategoryDto));
    }

    @PatchMapping("/{therapeuticCategoryId}/status")
    public ResponseEntity<TherapeuticCategoryDto> updateTherapeuticCategoryStatus(
            @PathVariable Long therapeuticCategoryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                therapeuticCategoryService.updateTherapeuticCategoryStatus(
                        therapeuticCategoryId, request.getIsActive()));
    }
}
