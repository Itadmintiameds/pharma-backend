package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class PharmacyOrganizationController {

    private final PharmacyOrganizationService organizationService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrganization(
            @RequestBody PharmacyOrganizationDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PharmacyOrganizationDto response =
                organizationService.createOrganization(
                        dto,
                        currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long userId) {

        organizationService.rejectRequest(userId);

        return ResponseEntity.ok("Request rejected successfully.");
    }
}