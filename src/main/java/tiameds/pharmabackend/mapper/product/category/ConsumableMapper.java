package tiameds.pharmabackend.mapper.product.category;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import tiameds.pharmabackend.dto.product.ProductAttributeConsumableMedicalDto;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.entity.master.MaterialType;
import tiameds.pharmabackend.entity.product.ProductAttributeConsumableMedical;

@Component
public class ConsumableMapper {

    public ProductAttributeConsumableMedical toEntity(ProductAttributeConsumableMedicalDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        ProductAttributeConsumableMedical entity = new ProductAttributeConsumableMedical();
        
        if (dto.getDeviceCategoryId() != null) {
            DeviceCategory dc = new DeviceCategory();
            dc.setDeviceCategoryId(dto.getDeviceCategoryId());
            entity.setDeviceCategory(dc);
        }
        if (dto.getDeviceSubCategoryId() != null) {
            DeviceSubCategory dsc = new DeviceSubCategory();
            dsc.setDeviceSubCategoryId(dto.getDeviceSubCategoryId());
            entity.setDeviceSubCategory(dsc);
        }
        
        if (dto.getMaterialTypeIds() != null) {
            List<MaterialType> list = new ArrayList<>();
            for (Long id : dto.getMaterialTypeIds()) {
                MaterialType mt = new MaterialType();
                mt.setMaterialTypeId(id);
                list.add(mt);
            }
            entity.setMaterialTypes(list);
        }
        
        if (dto.getDeviceSpecificationUnitId() != null) {
            DeviceSpecificationUnit dsu = new DeviceSpecificationUnit();
            dsu.setDeviceSpecificationUnitId(dto.getDeviceSpecificationUnitId());
            entity.setDeviceSpecificationUnit(dsu);
        }

        entity.setDimensionSize(dto.getDimensionSize());
        entity.setSterileOrNonSterile(dto.getSterileOrNonSterile());
        entity.setDisposalOrNonDisposal(dto.getDisposalOrNonDisposal());
        entity.setPurpose(dto.getPurpose());
        entity.setManufacturerName(dto.getManufacturerName());
        entity.setManufacturerLicenseNumber(dto.getManufacturerLicenseNumber());
        entity.setIsISOCertified(dto.getIsISOCertified());
        
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public ProductAttributeConsumableMedicalDto toDto(ProductAttributeConsumableMedical entity) {
        if (entity == null) return null;
        ProductAttributeConsumableMedicalDto dto = new ProductAttributeConsumableMedicalDto();
        dto.setProductAttributeId(entity.getProductAttributeId());
        
        if (entity.getDeviceCategory() != null) dto.setDeviceCategoryId(entity.getDeviceCategory().getDeviceCategoryId());
        if (entity.getDeviceSubCategory() != null) dto.setDeviceSubCategoryId(entity.getDeviceSubCategory().getDeviceSubCategoryId());
        if (entity.getMaterialTypes() != null) {
            dto.setMaterialTypeIds(entity.getMaterialTypes().stream().map(MaterialType::getMaterialTypeId).collect(Collectors.toList()));
        }
        
        if (entity.getDeviceSpecificationUnit() != null) dto.setDeviceSpecificationUnitId(entity.getDeviceSpecificationUnit().getDeviceSpecificationUnitId());

        dto.setDimensionSize(entity.getDimensionSize());
        dto.setSterileOrNonSterile(entity.getSterileOrNonSterile());
        dto.setDisposalOrNonDisposal(entity.getDisposalOrNonDisposal());
        dto.setPurpose(entity.getPurpose());
        dto.setManufacturerName(entity.getManufacturerName());
        dto.setManufacturerLicenseNumber(entity.getManufacturerLicenseNumber());
        dto.setIsISOCertified(entity.getIsISOCertified());
        
        return dto;
    }

}
