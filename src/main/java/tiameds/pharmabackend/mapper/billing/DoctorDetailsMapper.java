package tiameds.pharmabackend.mapper.billing;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.billing.DoctorDetailsDto;
import tiameds.pharmabackend.entity.billing.DoctorDetails;

@Component
public class DoctorDetailsMapper {

    // Entity -> DTO
    public DoctorDetailsDto toDto(DoctorDetails entity) {
        if (entity == null) {
            return null;
        }

        DoctorDetailsDto dto = new DoctorDetailsDto();
        dto.setDoctorId(entity.getDoctorId());
        dto.setPharmacyId(entity.getPharmacyId());
        dto.setDoctorName(entity.getDoctorName());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    // DTO -> Entity
    public DoctorDetails toEntity(DoctorDetailsDto dto) {
        if (dto == null) {
            return null;
        }

        DoctorDetails entity = new DoctorDetails();
        entity.setDoctorId(dto.getDoctorId());
        entity.setPharmacyId(dto.getPharmacyId());
        entity.setDoctorName(dto.getDoctorName());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}
