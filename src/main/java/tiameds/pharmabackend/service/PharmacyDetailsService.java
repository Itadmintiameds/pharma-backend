package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.LoggedInUserPharmacyDto;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.dto.PharmacySummaryDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface PharmacyDetailsService {

    PharmacyDetailsDto createPharmacy(PharmacyDetailsDto pharmacyDetailsDto);

    // OLD: pharmacyId came from the frontend as a path variable.
    // PharmacyDetailsDto getPharmacyById(String pharmacyId);
    PharmacyDetailsDto getCurrentPharmacy(String currentUserId);

    List<PharmacySummaryDto> getPharmacyCitiesOfTheOrganization(String currentUserId);

    List<LoggedInUserPharmacyDto> getLoggedInUserPharmacies(String userId);
}
