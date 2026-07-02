package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class PharmacyOrganizationController {

    private final PharmacyOrganizationService organizationService;

    @PostMapping("/create")
    public ResponseEntity<PharmacyOrganizationDto> createOrganization(
            @RequestBody PharmacyOrganizationDto dto) {

        PharmacyOrganizationDto response =
                organizationService.createOrganization(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}