package tiameds.pharmabackend.mapper.supplier;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;

@Component
public class SupplierMasterMapper {

    // Entity -> DTO
    public SupplierMasterDto toDto(SupplierMaster entity) {
        if (entity == null) {
            return null;
        }

        SupplierMasterDto dto = new SupplierMasterDto();
        dto.setSupplierId(entity.getSupplierId());
        dto.setPharmacyId(entity.getPharmacyId());
        dto.setWarehouseId(entity.getWarehouseId());
        dto.setSupplierName(entity.getSupplierName());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    // DTO -> Entity
    public SupplierMaster toEntity(SupplierMasterDto dto) {
        if (dto == null) {
            return null;
        }

        SupplierMaster entity = new SupplierMaster();
        entity.setSupplierId(dto.getSupplierId());
        entity.setPharmacyId(dto.getPharmacyId());
        entity.setSupplierName(dto.getSupplierName());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }

}