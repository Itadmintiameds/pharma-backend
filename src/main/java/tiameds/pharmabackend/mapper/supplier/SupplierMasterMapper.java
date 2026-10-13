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
        dto.setDlno(entity.getDlno());
        dto.setGstinNo(entity.getGstinNo());
        dto.setPanNo(entity.getPanNo());
        dto.setDlExpiryDate(entity.getDlExpiryDate());
        dto.setIssuingAuthority(entity.getIssuingAuthority());
        dto.setFssaiNo(entity.getFssaiNo());
        dto.setContactPersonName(entity.getContactPersonName());
        dto.setMobileNumber(entity.getMobileNumber());
        dto.setSupplierEmail(entity.getSupplierEmail());
        dto.setAddress(entity.getAddress());
        dto.setBuildingNo(entity.getBuildingNo());
        dto.setPincode(entity.getPincode());
        dto.setCity(entity.getCity());
        dto.setDistrict(entity.getDistrict());
        dto.setState(entity.getState());
        dto.setBankName(entity.getBankName());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setIfscCode(entity.getIfscCode());
        dto.setStatus(entity.getStatus());
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
        entity.setWarehouseId(dto.getWarehouseId());
        entity.setSupplierName(dto.getSupplierName());
        entity.setDlno(dto.getDlno());
        entity.setGstinNo(dto.getGstinNo());
        entity.setPanNo(dto.getPanNo());
        entity.setDlExpiryDate(dto.getDlExpiryDate());
        entity.setIssuingAuthority(dto.getIssuingAuthority());
        entity.setFssaiNo(dto.getFssaiNo());
        entity.setContactPersonName(dto.getContactPersonName());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setSupplierEmail(dto.getSupplierEmail());
        entity.setAddress(dto.getAddress());
        entity.setBuildingNo(dto.getBuildingNo());
        entity.setPincode(dto.getPincode());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setState(dto.getState());
        entity.setBankName(dto.getBankName());
        entity.setAccountHolderName(dto.getAccountHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setStatus(dto.getStatus());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }

}