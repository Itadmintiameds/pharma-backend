package tiameds.pharmabackend.service;

import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;

public interface PharmacyOrganizationService {

    PharmacyOrganizationDto createOrganization(PharmacyOrganizationDto organizationDto, UserDetails user);

    void rejectRequest(String userId);

    PharmacyOrganization getUserOrganization(String userId);

    PharmacyOrganizationDto uploadOrganizationLogo(String userId, MultipartFile logo);

}
