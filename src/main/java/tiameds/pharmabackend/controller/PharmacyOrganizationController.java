package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.OrganizationDeletionService;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class PharmacyOrganizationController {

    private final PharmacyOrganizationService organizationService;
    private final OrganizationDeletionService organizationDeletionService;

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
            @PathVariable String userId) {

        organizationService.rejectRequest(userId);

        return ResponseEntity.ok("Request rejected successfully.");
    }

    @GetMapping("/getUserOrganization")
    public ResponseEntity<PharmacyOrganization> getMyOrganization(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PharmacyOrganization organization =
                organizationService.getUserOrganization(userDetails.getUserId());

        return ResponseEntity.ok(organization);
    }

    // Hard-deletes an organization and EVERYTHING under it: pharmacies, warehouses,
    // users, products (with all children), inventory, purchases and suppliers. Irreversible.
    @DeleteMapping("/delete-all-data/{organizationId}")
    public ResponseEntity<?> deleteOrganizationData(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Integer> deletedCounts =
                organizationDeletionService.deleteOrganizationData(organizationId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "All data for organization " + organizationId + " has been deleted.");
        response.put("deletedCounts", deletedCounts);

        return ResponseEntity.ok(response);
    }
}