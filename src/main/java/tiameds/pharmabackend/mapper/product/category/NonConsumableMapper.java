package tiameds.pharmabackend.mapper.product.category;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.ProductAttributeNonConsumableMedicalDto;
import tiameds.pharmabackend.entity.master.Country;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.entity.master.MaterialType;
import tiameds.pharmabackend.entity.master.PowerSource;
import tiameds.pharmabackend.entity.product.ProductAttributeNonConsumableMedical;

@Component
public class NonConsumableMapper {

    public ProductAttributeNonConsumableMedical toEntity(ProductAttributeNonConsumableMedicalDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        ProductAttributeNonConsumableMedical entity = new ProductAttributeNonConsumableMedical();
        
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
        if (dto.getPowerSourceId() != null) {
            PowerSource ps = new PowerSource();
            ps.setPowerSourceId(dto.getPowerSourceId());
            entity.setPowerSource(ps);
        }
        if (dto.getCountryId() != null) {
            Country c = new Country();
            c.setCountryId(dto.getCountryId());
            entity.setCountryMaster(c);
        }
        
        if (dto.getDeviceSpecificationUnitId() != null) {
            DeviceSpecificationUnit dsu = new DeviceSpecificationUnit();
            dsu.setDeviceSpecificationUnitId(dto.getDeviceSpecificationUnitId());
            entity.setDeviceSpecificationUnit(dsu);
        }

        entity.setModelName(dto.getModelName());
        entity.setDeviceClassification(dto.getDeviceClassification());
        entity.setPurpose(dto.getPurpose());
        entity.setDimensionSize(dto.getDimensionSize());
        entity.setWarrantyPeriod(dto.getWarrantyPeriod());
        entity.setServiceAvailability(dto.getServiceAvailability());
        entity.setManufacturerName(dto.getManufacturerName());
        
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public ProductAttributeNonConsumableMedicalDto toDto(ProductAttributeNonConsumableMedical entity) {
        if (entity == null) return null;
        ProductAttributeNonConsumableMedicalDto dto = new ProductAttributeNonConsumableMedicalDto();
        dto.setProductAttributeId(entity.getProductAttributeId());
        
        if (entity.getDeviceCategory() != null) dto.setDeviceCategoryId(entity.getDeviceCategory().getDeviceCategoryId());
        if (entity.getDeviceSubCategory() != null) dto.setDeviceSubCategoryId(entity.getDeviceSubCategory().getDeviceSubCategoryId());
        if (entity.getMaterialTypes() != null) {
            dto.setMaterialTypeIds(entity.getMaterialTypes().stream().map(MaterialType::getMaterialTypeId).collect(Collectors.toList()));
        }
        if (entity.getPowerSource() != null) dto.setPowerSourceId(entity.getPowerSource().getPowerSourceId());
        if (entity.getCountryMaster() != null) dto.setCountryId(entity.getCountryMaster().getCountryId());
        
        if (entity.getDeviceSpecificationUnit() != null) dto.setDeviceSpecificationUnitId(entity.getDeviceSpecificationUnit().getDeviceSpecificationUnitId());

        dto.setModelName(entity.getModelName());
        dto.setDeviceClassification(entity.getDeviceClassification());
        dto.setPurpose(entity.getPurpose());
        dto.setDimensionSize(entity.getDimensionSize());
        dto.setWarrantyPeriod(entity.getWarrantyPeriod());
        dto.setServiceAvailability(entity.getServiceAvailability());
        dto.setManufacturerName(entity.getManufacturerName());
        
        return dto;
    }

}
