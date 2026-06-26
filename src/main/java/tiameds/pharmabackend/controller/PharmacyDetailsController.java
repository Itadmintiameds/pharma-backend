package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.service.PharmacyDetailsService;

@RestController
@RequestMapping("/pharmacy")
@RequiredArgsConstructor
public class PharmacyDetailsController {

    private final PharmacyDetailsService pharmacyDetailsService;

    @PostMapping("/create")
    public ResponseEntity<PharmacyDetailsDto> createPharmacy(
            @RequestBody PharmacyDetailsDto pharmacyDetailsDto) {

        PharmacyDetailsDto response =
                pharmacyDetailsService.createPharmacy(
                        pharmacyDetailsDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}