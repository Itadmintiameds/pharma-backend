package tiameds.pharmabackend.mapper;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;

import java.util.stream.Collectors;

@Component
public class PharmacyOrganizationMapper {

    public PharmacyOrganization toEntity(PharmacyOrganizationDto dto){

        if(dto == null){
            return null;
        }

        PharmacyOrganization organization = new PharmacyOrganization();

        organization.setOrganizationId(dto.getOrganizationId());
        organization.setOrganizationName(dto.getOrganizationName());
        organization.setOrganizationType(dto.getOrganizationType());
        organization.setOwnershipType(dto.getOwnershipType());
        organization.setPanNumber(dto.getPanNumber());
        organization.setGstNumber(dto.getGstNumber());
        organization.setCreatedAt(dto.getCreatedAt());
        organization.setIsActive(dto.getIsActive());
        organization.setIsRejected(dto.getIsRejected());

        return organization;
    }

    public PharmacyOrganizationDto toDto(PharmacyOrganization entity){

        if(entity == null){
            return null;
        }

        PharmacyOrganizationDto dto = new PharmacyOrganizationDto();

        dto.setOrganizationId(entity.getOrganizationId());
        dto.setOrganizationName(entity.getOrganizationName());
        dto.setOrganizationType(entity.getOrganizationType());
        dto.setOwnershipType(entity.getOwnershipType());
        dto.setPanNumber(entity.getPanNumber());
        dto.setGstNumber(entity.getGstNumber());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setIsActive(entity.getIsActive());
        dto.setIsRejected(entity.getIsRejected());

        return dto;
    }
}