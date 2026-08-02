package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.CountryDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.CountryService;

import java.util.List;

@RestController
@RequestMapping("/master/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    public ResponseEntity<List<CountryDto>> getAllCountries() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @GetMapping("/{countryId}")
    public ResponseEntity<CountryDto> getCountryById(@PathVariable Long countryId) {
        return ResponseEntity.ok(countryService.getCountryById(countryId));
    }

    @PostMapping
    public ResponseEntity<CountryDto> createCountry(@RequestBody CountryDto countryDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(countryService.createCountry(countryDto));
    }

    @PutMapping("/{countryId}")
    public ResponseEntity<CountryDto> updateCountry(
            @PathVariable Long countryId,
            @RequestBody CountryDto countryDto) {

        return ResponseEntity.ok(countryService.updateCountry(countryId, countryDto));
    }

    @PatchMapping("/{countryId}/status")
    public ResponseEntity<CountryDto> updateCountryStatus(
            @PathVariable Long countryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                countryService.updateCountryStatus(countryId, request.getIsActive()));
    }
}
