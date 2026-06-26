package tiameds.pharmabackend.mapper;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;

@Component
public class PharmacyDetailsMapper {

    public PharmacyDetails toEntity(PharmacyDetailsDto dto) {

        PharmacyDetails pharmacy = new PharmacyDetails();

        pharmacy.setPharmacyId(dto.getPharmacyId());
        pharmacy.setPharmacyRegistrationId(dto.getPharmacyRegistrationId());
        pharmacy.setPharmacyName(dto.getPharmacyName());
        pharmacy.setPharmacyType(dto.getPharmacyType());
        pharmacy.setPharmacyEmail(dto.getPharmacyEmail());
        pharmacy.setPharmacyPhone(dto.getPharmacyPhone());
        pharmacy.setPharmacyDlno(dto.getPharmacyDlno());
        pharmacy.setPharmacyGstno(dto.getPharmacyGstno());
        pharmacy.setPharmacyPan(dto.getPharmacyPan());
        pharmacy.setPharmacyBusinessRegistrationNo(
                dto.getPharmacyBusinessRegistrationNo());
        pharmacy.setPharmacyDlExpiryDate(
                dto.getPharmacyDlExpiryDate());
        pharmacy.setPharmacyAddress(dto.getPharmacyAddress());
        pharmacy.setPharmacyLogo(dto.getPharmacyLogo());
        pharmacy.setCreatedBy(dto.getCreatedBy());
        pharmacy.setCreatedAt(dto.getCreatedAt());
        pharmacy.setModifiedBy(dto.getModifiedBy());
        pharmacy.setModifiedAt(dto.getModifiedAt());

        return pharmacy;
    }

    public PharmacyDetailsDto toDto(PharmacyDetails pharmacy) {

        PharmacyDetailsDto dto = new PharmacyDetailsDto();

        dto.setPharmacyId(pharmacy.getPharmacyId());
        dto.setPharmacyRegistrationId(
                pharmacy.getPharmacyRegistrationId());
        dto.setPharmacyName(pharmacy.getPharmacyName());
        dto.setPharmacyType(pharmacy.getPharmacyType());
        dto.setPharmacyEmail(pharmacy.getPharmacyEmail());
        dto.setPharmacyPhone(pharmacy.getPharmacyPhone());
        dto.setPharmacyDlno(pharmacy.getPharmacyDlno());
        dto.setPharmacyGstno(pharmacy.getPharmacyGstno());
        dto.setPharmacyPan(pharmacy.getPharmacyPan());
        dto.setPharmacyBusinessRegistrationNo(
                pharmacy.getPharmacyBusinessRegistrationNo());
        dto.setPharmacyDlExpiryDate(
                pharmacy.getPharmacyDlExpiryDate());
        dto.setPharmacyAddress(pharmacy.getPharmacyAddress());
        dto.setPharmacyLogo(pharmacy.getPharmacyLogo());
        dto.setCreatedBy(pharmacy.getCreatedBy());
        dto.setCreatedAt(pharmacy.getCreatedAt());
        dto.setModifiedBy(pharmacy.getModifiedBy());
        dto.setModifiedAt(pharmacy.getModifiedAt());

        return dto;
    }
}