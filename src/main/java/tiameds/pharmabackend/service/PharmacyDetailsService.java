package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.LoggedInUserPharmacyDto;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.dto.PharmacySummaryDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface PharmacyDetailsService {

    PharmacyDetailsDto createPharmacy(PharmacyDetailsDto pharmacyDetailsDto);

    List<PharmacySummaryDto> getPharmacyCitiesOfTheOrganization(Long currentUserId);

    List<LoggedInUserPharmacyDto> getLoggedInUserPharmacies(Long userId);
}
