package tiameds.pharmabackend.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PharmacyDetailsMapper {

    private final PharmaDocumentsMapper pharmaDocumentsMapper;
    private final PharmacyOrganizationMapper pharmacyOrganizationMapper;

    public PharmacyDetails toEntity(PharmacyDetailsDto dto) {

        if (dto == null) {
            return null;
        }

        PharmacyDetails pharmacy = new PharmacyDetails();

        pharmacy.setPharmacyId(dto.getPharmacyId());
        pharmacy.setPharmacyRegistrationId(dto.getPharmacyRegistrationId());
        pharmacy.setPharmacyName(dto.getPharmacyName());
        pharmacy.setPharmacyType(dto.getPharmacyType());
        pharmacy.setPharmacyEmail(dto.getPharmacyEmail());
        pharmacy.setPharmacyPhone(dto.getPharmacyPhone());
        pharmacy.setPanNumber(dto.getPanNumber());
        pharmacy.setGstNumber(dto.getGstNumber());
        pharmacy.setPharmacyBranch(dto.getPharmacyBranch());
        pharmacy.setPharmacyBuildingNo(dto.getPharmacyBuildingNo());
        pharmacy.setPharmacyStreet(dto.getPharmacyStreet());
        pharmacy.setPharmacyCity(dto.getPharmacyCity());
        pharmacy.setPharmacyTaluka(dto.getPharmacyTaluka());
        pharmacy.setPharmacyDistricts(dto.getPharmacyDistricts());
        pharmacy.setPharmacyPincode(dto.getPharmacyPincode());
        pharmacy.setPharmacyLandmark(dto.getPharmacyLandmark());
        pharmacy.setPharmacyState(dto.getPharmacyState());
        pharmacy.setPharmacyLogo(dto.getPharmacyLogo());
        pharmacy.setCreatedBy(dto.getCreatedBy());
        pharmacy.setCreatedAt(dto.getCreatedAt());
        pharmacy.setModifiedBy(dto.getModifiedBy());
        pharmacy.setModifiedAt(dto.getModifiedAt());

        if (dto.getDocuments() != null) {
            pharmacy.setDocuments(
                    dto.getDocuments()
                            .stream()
                            .map(pharmaDocumentsMapper::toEntity)
                            .peek(doc -> doc.setPharmacy(pharmacy))
                            .collect(Collectors.toList())
            );
        }

        return pharmacy;
    }

    public PharmacyDetailsDto toDto(PharmacyDetails pharmacy) {

        if (pharmacy == null) {
            return null;
        }

        PharmacyDetailsDto dto = new PharmacyDetailsDto();

        dto.setPharmacyId(pharmacy.getPharmacyId());
        dto.setPharmacyRegistrationId(pharmacy.getPharmacyRegistrationId());
        dto.setPharmacyName(pharmacy.getPharmacyName());
        dto.setPharmacyType(pharmacy.getPharmacyType());
        dto.setPharmacyEmail(pharmacy.getPharmacyEmail());
        dto.setPharmacyPhone(pharmacy.getPharmacyPhone());
        dto.setPanNumber(pharmacy.getPanNumber());
        dto.setGstNumber(pharmacy.getGstNumber());
        dto.setPharmacyBranch(pharmacy.getPharmacyBranch());
        dto.setPharmacyBuildingNo(pharmacy.getPharmacyBuildingNo());
        dto.setPharmacyStreet(pharmacy.getPharmacyStreet());
        dto.setPharmacyCity(pharmacy.getPharmacyCity());
        dto.setPharmacyTaluka(pharmacy.getPharmacyTaluka());
        dto.setPharmacyDistricts(pharmacy.getPharmacyDistricts());
        dto.setPharmacyPincode(pharmacy.getPharmacyPincode());
        dto.setPharmacyLandmark(pharmacy.getPharmacyLandmark());
        dto.setPharmacyState(pharmacy.getPharmacyState());
        dto.setPharmacyLogo(pharmacy.getPharmacyLogo());
        dto.setCreatedBy(pharmacy.getCreatedBy());
        dto.setCreatedAt(pharmacy.getCreatedAt());
        dto.setModifiedBy(pharmacy.getModifiedBy());
        dto.setModifiedAt(pharmacy.getModifiedAt());

        // Organization mapping (summary)
        dto.setPharmacyOrganization(pharmacyOrganizationMapper.toDto(pharmacy.getOrganization()));

        // User mapping (summary)
        if (pharmacy.getUsers() != null) {
            dto.setUsers(
                    pharmacy.getUsers()
                            .stream()
                            .map(this::toUserSummaryDto)
                            .collect(Collectors.toList())
            );
        }

        if (pharmacy.getDocuments() != null) {
            dto.setDocuments(
                    pharmacy.getDocuments()
                            .stream()
                            .map(pharmaDocumentsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    private UserDetailsDto toUserSummaryDto(UserDetails user) {

        UserDetailsDto dto = new UserDetailsDto();

        dto.setUserId(user.getUserId());
        dto.setUserEmail(user.getUserEmail());
        dto.setUserStatus(user.getUserStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setModifiedBy(user.getModifiedBy());
        dto.setModifiedAt(user.getModifiedAt());

        if (user.getRole() != null) {
            PharmaRolesDto roleDto = new PharmaRolesDto();
            roleDto.setRoleId(user.getRole().getRoleId());
            roleDto.setRoleName(user.getRole().getRoleName());
            dto.setPharmaRolesDto(roleDto);
        }

        // Don't map pharmacies again.
        return dto;
    }
}