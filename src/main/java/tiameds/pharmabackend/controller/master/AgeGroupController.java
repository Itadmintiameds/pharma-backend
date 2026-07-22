package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.AgeGroupDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.AgeGroupService;

import java.util.List;

@RestController
@RequestMapping("/master/age-groups")
@RequiredArgsConstructor
public class AgeGroupController {

    private final AgeGroupService ageGroupService;

    @GetMapping
    public ResponseEntity<List<AgeGroupDto>> getAllAgeGroups() {
        return ResponseEntity.ok(ageGroupService.getAllAgeGroups());
    }

    @GetMapping("/{ageGroupId}")
    public ResponseEntity<AgeGroupDto> getAgeGroupById(@PathVariable Long ageGroupId) {
        return ResponseEntity.ok(ageGroupService.getAgeGroupById(ageGroupId));
    }

    @PostMapping
    public ResponseEntity<AgeGroupDto> createAgeGroup(@RequestBody AgeGroupDto ageGroupDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ageGroupService.createAgeGroup(ageGroupDto));
    }

    @PutMapping("/{ageGroupId}")
    public ResponseEntity<AgeGroupDto> updateAgeGroup(
            @PathVariable Long ageGroupId,
            @RequestBody AgeGroupDto ageGroupDto) {

        return ResponseEntity.ok(ageGroupService.updateAgeGroup(ageGroupId, ageGroupDto));
    }

    @PatchMapping("/{ageGroupId}/status")
    public ResponseEntity<AgeGroupDto> updateAgeGroupStatus(
            @PathVariable Long ageGroupId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                ageGroupService.updateAgeGroupStatus(ageGroupId, request.getIsActive()));
    }
}
