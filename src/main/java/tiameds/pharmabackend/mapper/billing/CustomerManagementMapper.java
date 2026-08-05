package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.CustomerManagementDto;
import tiameds.pharmabackend.entity.billing.CustomerManagement;

public class CustomerManagementMapper {

    public static CustomerManagementDto toDto(CustomerManagement entity) {

        if (entity == null) {
            return null;
        }

        CustomerManagementDto dto = new CustomerManagementDto();

        dto.setCustomerId(entity.getCustomerId());
        dto.setPharmacyId(
                entity.getPharmacy() != null
                        ? entity.getPharmacy().getPharmacyId()
                        : null
        );
        dto.setCustomerName(entity.getCustomerName());
        dto.setCustomerPhoneNo(entity.getCustomerPhoneNo());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static CustomerManagement toEntity(CustomerManagementDto dto) {

        if (dto == null) {
            return null;
        }

        CustomerManagement entity = new CustomerManagement();

        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerName(dto.getCustomerName());
        entity.setCustomerPhoneNo(dto.getCustomerPhoneNo());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}