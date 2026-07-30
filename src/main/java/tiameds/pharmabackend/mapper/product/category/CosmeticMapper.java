package tiameds.pharmabackend.mapper.product.category;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import tiameds.pharmabackend.dto.product.ProductAttributeCosmeticsDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.HairType;
import tiameds.pharmabackend.entity.master.IntendedUseArea;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.entity.master.SkinType;
import tiameds.pharmabackend.entity.product.ProductAttributeCosmetics;

@Component
public class CosmeticMapper {

    public ProductAttributeCosmetics toEntity(ProductAttributeCosmeticsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        ProductAttributeCosmetics entity = new ProductAttributeCosmetics();
        
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
        
        if (dto.getIntendedUseAreaIds() != null) {
            List<IntendedUseArea> list = new ArrayList<>();
            for (Long id : dto.getIntendedUseAreaIds()) {
                IntendedUseArea item = new IntendedUseArea();
                item.setIntendedUseAreaId(id);
                list.add(item);
            }
            entity.setIntendedUseArea(list);
        }
        
        if (dto.getSkinTypeIds() != null) {
            List<SkinType> list = new ArrayList<>();
            for (Long id : dto.getSkinTypeIds()) {
                SkinType item = new SkinType();
                item.setSkinTypeId(id);
                list.add(item);
            }
            entity.setSkinType(list);
        }
        
        if (dto.getHairTypeIds() != null) {
            List<HairType> list = new ArrayList<>();
            for (Long id : dto.getHairTypeIds()) {
                HairType item = new HairType();
                item.setHairTypeId(id);
                list.add(item);
            }
            entity.setHairTypes(list);
        }
        
        if (dto.getAgeGroupIds() != null) {
            List<AgeGroup> list = new ArrayList<>();
            for (Long id : dto.getAgeGroupIds()) {
                AgeGroup item = new AgeGroup();
                item.setAgeGroupId(id);
                list.add(item);
            }
            entity.setAgeGroups(list);
        }
        
        entity.setGender(dto.getGender());
        entity.setFragrance(dto.getFragrance());
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

    public ProductAttributeCosmeticsDto toDto(ProductAttributeCosmetics entity) {
        if (entity == null) return null;
        ProductAttributeCosmeticsDto dto = new ProductAttributeCosmeticsDto();
        dto.setProductAttributeId(entity.getProductAttributeId());
        
        if (entity.getProductType() != null) dto.setProductTypeId(entity.getProductType().getProductTypeId());
        if (entity.getProductSubType() != null) dto.setProductSubTypeId(entity.getProductSubType().getProductSubTypeId());
        if (entity.getProductForm() != null) dto.setProductFormId(entity.getProductForm().getProductFormId());
        
        dto.setVariantName(entity.getVariantName());
        
        if (entity.getIntendedUseArea() != null) {
            dto.setIntendedUseAreaIds(entity.getIntendedUseArea().stream().map(IntendedUseArea::getIntendedUseAreaId).collect(Collectors.toList()));
        }
        if (entity.getSkinType() != null) {
            dto.setSkinTypeIds(entity.getSkinType().stream().map(SkinType::getSkinTypeId).collect(Collectors.toList()));
        }
        if (entity.getHairTypes() != null) {
            dto.setHairTypeIds(entity.getHairTypes().stream().map(HairType::getHairTypeId).collect(Collectors.toList()));
        }
        if (entity.getAgeGroups() != null) {
            dto.setAgeGroupIds(entity.getAgeGroups().stream().map(AgeGroup::getAgeGroupId).collect(Collectors.toList()));
        }
        
        dto.setGender(entity.getGender());
        dto.setFragrance(entity.getFragrance());
        dto.setNetQuantity(entity.getNetQuantity());
        if (entity.getNetQuantityUnit() != null) dto.setNetQuantityUnitId(entity.getNetQuantityUnit().getNetQuantityUnitId());
        dto.setManufacturerName(entity.getManufacturerName());
        
        return dto;
    }

}
