package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.TherapeuticSubcategoryDto;
import tiameds.pharmabackend.service.master.TherapeuticSubcategoryService;

import java.util.List;

@RestController
@RequestMapping("/master/therapeutic-subcategories")
@RequiredArgsConstructor
public class TherapeuticSubcategoryController {

    private final TherapeuticSubcategoryService therapeuticSubcategoryService;

    @GetMapping
    public ResponseEntity<List<TherapeuticSubcategoryDto>> getAllTherapeuticSubcategories() {
        return ResponseEntity.ok(therapeuticSubcategoryService.getAllTherapeuticSubcategories());
    }

    @GetMapping("/{therapeuticSubcategoryId}")
    public ResponseEntity<TherapeuticSubcategoryDto> getTherapeuticSubcategoryById(
            @PathVariable Long therapeuticSubcategoryId) {

        return ResponseEntity.ok(
                therapeuticSubcategoryService.getTherapeuticSubcategoryById(therapeuticSubcategoryId));
    }

    @PostMapping
    public ResponseEntity<TherapeuticSubcategoryDto> createTherapeuticSubcategory(
            @RequestBody TherapeuticSubcategoryDto therapeuticSubcategoryDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(therapeuticSubcategoryService.createTherapeuticSubcategory(therapeuticSubcategoryDto));
    }

    @PutMapping("/{therapeuticSubcategoryId}")
    public ResponseEntity<TherapeuticSubcategoryDto> updateTherapeuticSubcategory(
            @PathVariable Long therapeuticSubcategoryId,
            @RequestBody TherapeuticSubcategoryDto therapeuticSubcategoryDto) {

        return ResponseEntity.ok(
                therapeuticSubcategoryService.updateTherapeuticSubcategory(
                        therapeuticSubcategoryId, therapeuticSubcategoryDto));
    }

    @PatchMapping("/{therapeuticSubcategoryId}/status")
    public ResponseEntity<TherapeuticSubcategoryDto> updateTherapeuticSubcategoryStatus(
            @PathVariable Long therapeuticSubcategoryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                therapeuticSubcategoryService.updateTherapeuticSubcategoryStatus(
                        therapeuticSubcategoryId, request.getIsActive()));
    }
}
