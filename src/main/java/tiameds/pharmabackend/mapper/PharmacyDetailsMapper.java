package tiameds.pharmabackend.mapper;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;

import java.util.stream.Collectors;

@Component
public class PharmacyDetailsMapper {

    private final UserDetailsMapper userDetailsMapper;
    private final PharmaDocumentsMapper pharmaDocumentsMapper;

    public PharmacyDetailsMapper(UserDetailsMapper userDetailsMapper,
                                 PharmaDocumentsMapper pharmaDocumentsMapper) {
        this.userDetailsMapper = userDetailsMapper;
        this.pharmaDocumentsMapper = pharmaDocumentsMapper;
    }

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

        if (dto.getUsers() != null) {
            pharmacy.setUsers(
                    dto.getUsers().stream()
                            .map(userDetailsMapper::toEntity)
                            .peek(user -> user.setPharmacy(pharmacy))
                            .collect(Collectors.toList())
            );
        }

        if (dto.getDocuments() != null) {
            pharmacy.setDocuments(
                    dto.getDocuments().stream()
                            .map(pharmaDocumentsMapper::toEntity)
                            .peek(document -> document.setPharmacy(pharmacy))
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

        if (pharmacy.getUsers() != null) {
            dto.setUsers(
                    pharmacy.getUsers().stream()
                            .map(userDetailsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        if (pharmacy.getDocuments() != null) {
            dto.setDocuments(
                    pharmacy.getDocuments().stream()
                            .map(pharmaDocumentsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}