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

    // True when the GSTIN is already in use.
    // forPharmacy = false (business GSTIN): taken if another organization, or
    //   a pharmacy of another organization, holds it.
    // forPharmacy = true (pharmacy GSTIN): taken if ANY pharmacy holds it (even
    //   one in the caller's own organization), another organization holds it,
    //   or it is saved as a supplier of the caller's own organization.
    boolean isGstNumberTaken(String gstNumber, String userId, boolean forPharmacy);

}
