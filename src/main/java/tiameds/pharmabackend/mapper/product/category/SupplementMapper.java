package tiameds.pharmabackend.mapper.product.category;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.ProductAttributeSupplementsDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.DosageForm;
import tiameds.pharmabackend.entity.master.Flavour;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;
import tiameds.pharmabackend.entity.product.ProductAttributeSupplements;

@Component
public class SupplementMapper {

    public ProductAttributeSupplements toEntity(ProductAttributeSupplementsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        ProductAttributeSupplements entity = new ProductAttributeSupplements();
        
        if (dto.getTherapeuticCategoryId() != null) {
            TherapeuticCategory tc = new TherapeuticCategory();
            tc.setTherapeuticCategoryId(dto.getTherapeuticCategoryId());
            entity.setTherapeuticCategory(tc);
        }
        
        if (dto.getTherapeuticSubcategoryId() != null) {
            TherapeuticSubcategory ts = new TherapeuticSubcategory();
            ts.setTherapeuticSubcategoryId(dto.getTherapeuticSubcategoryId());
            entity.setTherapeuticSubcategory(ts);
        }
        
        if (dto.getFlavourId() != null) {
            Flavour f = new Flavour();
            f.setFlavourId(dto.getFlavourId());
            entity.setFlavour(f);
        }
        
        if (dto.getDosageFormId() != null) {
            DosageForm df = new DosageForm();
            df.setDosageId(dto.getDosageFormId());
            entity.setDosageForm(df);
        }
        
        if (dto.getAgeGroupId() != null) {
            AgeGroup ag = new AgeGroup();
            ag.setAgeGroupId(dto.getAgeGroupId());
            entity.setAgeGroup(ag);
        }
        
        entity.setStrengthComposition(dto.getStrengthComposition());
        entity.setNetQuantity(dto.getNetQuantity());

        
        if (dto.getNetQuantityUnitId() != null) {
            NetQuantityUnit nqu = new NetQuantityUnit();
            nqu.setNetQuantityUnitId(dto.getNetQuantityUnitId());
            entity.setNetQuantityUnit(nqu);
        }
        

        entity.setGender(dto.getGender());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        entity.setManufacturerName(dto.getManufacturerName());
        entity.setFssaiLicenseNumber(dto.getFssaiLicenseNumber());
        
        
        return entity;
    }

    public ProductAttributeSupplementsDto toDto(ProductAttributeSupplements entity) {
        if (entity == null) return null;
        ProductAttributeSupplementsDto dto = new ProductAttributeSupplementsDto();
        dto.setProductAttributeId(entity.getProductAttributeId());
        if (entity.getTherapeuticCategory() != null) dto.setTherapeuticCategoryId(entity.getTherapeuticCategory().getTherapeuticCategoryId());
        if (entity.getTherapeuticSubcategory() != null) dto.setTherapeuticSubcategoryId(entity.getTherapeuticSubcategory().getTherapeuticSubcategoryId());
        if (entity.getFlavour() != null) dto.setFlavourId(entity.getFlavour().getFlavourId());
        if (entity.getDosageForm() != null) dto.setDosageFormId(entity.getDosageForm().getDosageId());
        if (entity.getAgeGroup() != null) dto.setAgeGroupId(entity.getAgeGroup().getAgeGroupId());
        
        dto.setStrengthComposition(entity.getStrengthComposition());
        dto.setNetQuantity(entity.getNetQuantity());

        if (entity.getNetQuantityUnit() != null) {
            dto.setNetQuantityUnitId(entity.getNetQuantityUnit().getNetQuantityUnitId());
        }

        dto.setGender(entity.getGender());
        dto.setManufacturerName(entity.getManufacturerName());
        dto.setFssaiLicenseNumber(entity.getFssaiLicenseNumber());
        return dto;
    }

}
