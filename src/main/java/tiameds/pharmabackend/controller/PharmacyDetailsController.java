package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.LoggedInUserPharmacyDto;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.dto.PharmacySummaryDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.PharmacyDetailsService;

import java.util.List;

@RestController
@RequestMapping("/pharmacy")
@RequiredArgsConstructor
public class PharmacyDetailsController {

    private final PharmacyDetailsService pharmacyDetailsService;

    @PostMapping("/create")
    public ResponseEntity<?> createPharmacy(
            @RequestBody PharmacyDetailsDto pharmacyDetailsDto) {

        PharmacyDetailsDto response =
                pharmacyDetailsService.createPharmacy(
                        pharmacyDetailsDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Pharmacy is taken from the X-Pharmacy-Id header (resolved into
    // CurrentPharmacyContext by CurrentPharmacyFilter), not from the path, and
    // the service verifies the logged-in user belongs to that pharmacy.
    @GetMapping("/getCurrentPharmacy")
    public ResponseEntity<PharmacyDetailsDto> getCurrentPharmacy(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                pharmacyDetailsService.getCurrentPharmacy(currentUser.getUserId()));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<PharmacySummaryDto>> getPharmacyCitiesOfTheOrganization(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                pharmacyDetailsService.getPharmacyCitiesOfTheOrganization(
                        currentUser.getUserId()));
    }

    @GetMapping("/userPharmacy")
    public ResponseEntity<List<LoggedInUserPharmacyDto>> getLoggedInUserPharmacies(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                pharmacyDetailsService.getLoggedInUserPharmacies(currentUser.getUserId())
        );
    }


//    @PostMapping("/create")
//    public ResponseEntity<PharmacyDetailsDto> createPharmacy(
//            @RequestBody PharmacyDetailsDto pharmacyDetailsDto) {
//
//        PharmacyDetailsDto response =
//                pharmacyDetailsService.createPharmacy(
//                        pharmacyDetailsDto);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(response);
//    }
}