package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.DosageFormDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.DosageFormService;

import java.util.List;

@RestController
@RequestMapping("/master/dosage-forms")
@RequiredArgsConstructor
public class DosageFormController {

    private final DosageFormService dosageFormService;

    @GetMapping
    public ResponseEntity<List<DosageFormDto>> getAllDosageForms() {
        return ResponseEntity.ok(dosageFormService.getAllDosageForms());
    }

    @GetMapping("/{dosageId}")
    public ResponseEntity<DosageFormDto> getDosageFormById(@PathVariable Long dosageId) {
        return ResponseEntity.ok(dosageFormService.getDosageFormById(dosageId));
    }

    @PostMapping
    public ResponseEntity<DosageFormDto> createDosageForm(@RequestBody DosageFormDto dosageFormDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dosageFormService.createDosageForm(dosageFormDto));
    }

    @PutMapping("/{dosageId}")
    public ResponseEntity<DosageFormDto> updateDosageForm(
            @PathVariable Long dosageId,
            @RequestBody DosageFormDto dosageFormDto) {

        return ResponseEntity.ok(dosageFormService.updateDosageForm(dosageId, dosageFormDto));
    }

    @PatchMapping("/{dosageId}/status")
    public ResponseEntity<DosageFormDto> updateDosageFormStatus(
            @PathVariable Long dosageId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                dosageFormService.updateDosageFormStatus(dosageId, request.getIsActive()));
    }
}
