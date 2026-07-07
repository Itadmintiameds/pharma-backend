package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface PharmacyOrganizationService {

    PharmacyOrganizationDto createOrganization(PharmacyOrganizationDto organizationDto, UserDetails user);

    void rejectRequest(Long userId);
}
