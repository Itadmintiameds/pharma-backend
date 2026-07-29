package tiameds.pharmabackend.mapper.product.category;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import tiameds.pharmabackend.dto.product.ProductAttributeFoodInfantDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.entity.product.ProductAttributeFoodInfant;

@Component
public class FoodInfantMapper {

    public ProductAttributeFoodInfant toEntity(ProductAttributeFoodInfantDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        ProductAttributeFoodInfant entity = new ProductAttributeFoodInfant();
        
        if (dto.getProductTypeId() != null) {
            ProductType pt = new ProductType();
            pt.setProductTypeId(dto.getProductTypeId());
            entity.setProductType(pt);
        }
        
        if (dto.getProductSubTypeId() != null) {
            ProductSubType pst = new ProductSubType();
            pst.setProductSubTypeId(dto.getProductSubTypeId());
            entity.setProductSubType(pst);
        }
        
        if (dto.getProductFormId() != null) {
            ProductForm pf = new ProductForm();
            pf.setProductFormId(dto.getProductFormId());
            entity.setProductForm(pf);
        }
        
        entity.setVariantName(dto.getVariantName());
        
        if (dto.getAgeGroupIds() != null) {
            List<AgeGroup> list = new ArrayList<>();
            for (Long id : dto.getAgeGroupIds()) {
                AgeGroup item = new AgeGroup();
                item.setAgeGroupId(id);
                list.add(item);
            }
            entity.setAgeGroups(list);
        }
        
        entity.setNetQuantity(dto.getNetQuantity());
        
        if (dto.getNetQuantityUnitId() != null) {
            NetQuantityUnit nqu = new NetQuantityUnit();
            nqu.setNetQuantityUnitId(dto.getNetQuantityUnitId());
            entity.setNetQuantityUnit(nqu);
        }
        
        entity.setManufacturerName(dto.getManufacturerName());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        
        return entity;
    }

    public ProductAttributeFoodInfantDto toDto(ProductAttributeFoodInfant entity) {
        if (entity == null) return null;
        ProductAttributeFoodInfantDto dto = new ProductAttributeFoodInfantDto();
        dto.setProductAttributeId(entity.getProductAttributeId());
        
        if (entity.getProductType() != null) dto.setProductTypeId(entity.getProductType().getProductTypeId());
        if (entity.getProductSubType() != null) dto.setProductSubTypeId(entity.getProductSubType().getProductSubTypeId());
        if (entity.getProductForm() != null) dto.setProductFormId(entity.getProductForm().getProductFormId());
        
        dto.setVariantName(entity.getVariantName());
        
        if (entity.getAgeGroups() != null) {
            dto.setAgeGroupIds(entity.getAgeGroups().stream().map(AgeGroup::getAgeGroupId).collect(Collectors.toList()));
        }
        
        dto.setNetQuantity(entity.getNetQuantity());
        if (entity.getNetQuantityUnit() != null) {
            dto.setNetQuantityUnitId(entity.getNetQuantityUnit().getNetQuantityUnitId());
        }
        dto.setManufacturerName(entity.getManufacturerName());
        
        return dto;
    }

}
