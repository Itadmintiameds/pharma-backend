package tiameds.pharmabackend.mapper.product;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.PackagingDetailsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeSupplementsDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.DosageForm;
import tiameds.pharmabackend.entity.master.Flavour;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductAttributeSupplements;
import tiameds.pharmabackend.entity.product.ProductDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import tiameds.pharmabackend.dto.product.ProductAttributeCosmeticsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductMoleculeDto;
import tiameds.pharmabackend.entity.master.HairType;
import tiameds.pharmabackend.entity.master.IntendedUseArea;
import tiameds.pharmabackend.entity.master.Molecule;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.entity.master.SkinType;
import tiameds.pharmabackend.entity.product.ProductAttributeCosmetics;
import tiameds.pharmabackend.entity.product.ProductAttributeDrug;
import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductMoleculeDto;
import tiameds.pharmabackend.entity.product.ProductMolecule;
import tiameds.pharmabackend.entity.master.Molecule;
import tiameds.pharmabackend.entity.PharmacyDetails;
@Component
public class ProductMapper {

    public ProductDetails toEntity(ProductDetailsDto dto, String generatedProductId, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        
        ProductDetails entity = new ProductDetails();
        entity.setProductId(generatedProductId);
        
        if (dto.getPharmacyId() != null) {
            PharmacyDetails pharmacy = new PharmacyDetails();
            pharmacy.setPharmacyId(dto.getPharmacyId());
            entity.setPharmacy(pharmacy);
        }
        
        if (dto.getProductCategoryId() != null) {
            ProductCategory cat = new ProductCategory();
            cat.setProductCategoryId(dto.getProductCategoryId());
            entity.setProductCategory(cat);
        }
        
        entity.setProductName(dto.getProductName());
        entity.setBrandName(dto.getBrandName());
        entity.setGstPercentage(dto.getGstPercentage());
        entity.setHsnNo(dto.getHsnNo());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        
        if (dto.getBatchDetails() != null) {
            List<BatchDetails> batchList = dto.getBatchDetails().stream().map(b -> toEntity(b, createdBy, createdAt)).collect(Collectors.toList());
            batchList.forEach(b -> b.setProduct(entity));
            entity.setBatchDetails(batchList);
        }
        
        if (dto.getPackagingDetails() != null) {
            List<PackagingDetails> packList = dto.getPackagingDetails().stream().map(p -> toEntity(p, createdBy, createdAt)).collect(Collectors.toList());
            packList.forEach(p -> p.setProduct(entity));
            entity.setPackagingDetails(packList);
        }
        
        if (dto.getProductAttributeSupplements() != null) {
            List<ProductAttributeSupplements> suppList = dto.getProductAttributeSupplements().stream().map(s -> toEntity(s, createdBy, createdAt)).collect(Collectors.toList());
            suppList.forEach(s -> s.setProduct(entity));
            entity.setProductAttributeSupplements(suppList);
        }
        
        if (dto.getProductAttributeCosmetics() != null) {
            List<ProductAttributeCosmetics> cosmList = dto.getProductAttributeCosmetics().stream().map(c -> toEntity(c, createdBy, createdAt)).collect(Collectors.toList());
            cosmList.forEach(c -> c.setProduct(entity));
            entity.setProductAttributeCosmetics(cosmList);
        }
        
        if (dto.getProductAttributeDrugs() != null) {
            List<ProductAttributeDrug> drugList = new ArrayList<>();
            for (ProductAttributeDrugDto dDto : dto.getProductAttributeDrugs()) {
                ProductAttributeDrug drugEntity = new ProductAttributeDrug();
                drugEntity.setDrugSchedule(dDto.getDrugSchedule());
                drugEntity.setCreatedBy(createdBy);
                drugEntity.setCreatedAt(createdAt);
                
                if (dDto.getProductMolecules() != null) {
                    List<ProductMolecule> molList = new ArrayList<>();
                    for (ProductMoleculeDto mDto : dDto.getProductMolecules()) {
                        ProductMolecule mol = new ProductMolecule();
                        mol.setMoleculeStrength(mDto.getMoleculeStrength());
                        if (mDto.getMoleculeId() != null) {
                            Molecule m = new Molecule();
                            m.setMoleculeId(mDto.getMoleculeId());
                            mol.setMolecule(m);
                        }
                        mol.setProductAttributeDrug(drugEntity);
                        molList.add(mol);
                    }
                    drugEntity.setProductMolecules(molList);
                }
                drugList.add(drugEntity);
            }
            drugList.forEach(d -> d.setProduct(entity));
            entity.setProductAttributeDrugs(drugList);
        }
        
        return entity;
    }

    private BatchDetails toEntity(BatchDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        BatchDetails entity = new BatchDetails();
        entity.setBatchNumber(dto.getBatchNumber());
        entity.setManufacturingDate(dto.getManufacturingDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setFreeUnit(dto.getFreeUnit());
        entity.setFreeQuantity(dto.getFreeQuantity());
        entity.setPurchasePrice(dto.getPurchasePrice());
        entity.setMrp(dto.getMrp());
        entity.setSellingPrice(dto.getSellingPrice());
        entity.setPurchasePricePerUnit(dto.getPurchasePricePerUnit());
        entity.setMrpPerUnit(dto.getMrpPerUnit());
        entity.setSellingPricePerUnit(dto.getSellingPricePerUnit());
        entity.setRackLocation(dto.getRackLocation());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private PackagingDetails toEntity(PackagingDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PackagingDetails entity = new PackagingDetails();
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setPurchaseUnitContains(dto.getPurchaseUnitContains());
        entity.setSmallestUnit(dto.getSmallestUnit());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private ProductAttributeSupplements toEntity(ProductAttributeSupplementsDto dto, String createdBy, LocalDateTime createdAt) {
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
        entity.setNetQuantityUnit(dto.getNetQuantityUnit());
        entity.setGender(dto.getGender());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        entity.setManufacturerName(dto.getManufacturerName());
        entity.setFssaiLicenseNumber(dto.getFssaiLicenseNumber());
        
        
        return entity;
    }

    private ProductAttributeCosmetics toEntity(ProductAttributeCosmeticsDto dto, String createdBy, LocalDateTime createdAt) {
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

    public ProductDetailsDto toDto(ProductDetails entity) {
        if (entity == null) return null;
        ProductDetailsDto dto = new ProductDetailsDto();
        dto.setProductId(entity.getProductId());
        
        if (entity.getPharmacy() != null) {
            dto.setPharmacyId(entity.getPharmacy().getPharmacyId());
        }
        if (entity.getProductCategory() != null) {
            dto.setProductCategoryId(entity.getProductCategory().getProductCategoryId());
        }
        
        dto.setProductName(entity.getProductName());
        dto.setBrandName(entity.getBrandName());
        dto.setGstPercentage(entity.getGstPercentage());
        dto.setHsnNo(entity.getHsnNo());
        
        if (entity.getBatchDetails() != null) {
            dto.setBatchDetails(entity.getBatchDetails().stream().map(this::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getPackagingDetails() != null) {
            dto.setPackagingDetails(entity.getPackagingDetails().stream().map(this::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeSupplements() != null) {
            dto.setProductAttributeSupplements(entity.getProductAttributeSupplements().stream().map(this::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeCosmetics() != null) {
            dto.setProductAttributeCosmetics(entity.getProductAttributeCosmetics().stream().map(this::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeDrugs() != null && !entity.getProductAttributeDrugs().isEmpty()) {
            dto.setProductAttributeDrugs(toDrugDtoList(entity.getProductAttributeDrugs()));
        }
        
        return dto;
    }

    private BatchDetailsDto toDto(BatchDetails entity) {
        if (entity == null) return null;
        BatchDetailsDto dto = new BatchDetailsDto();
        dto.setBatchId(entity.getBatchId());
        dto.setBatchNumber(entity.getBatchNumber());
        dto.setManufacturingDate(entity.getManufacturingDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setFreeUnit(entity.getFreeUnit());
        dto.setFreeQuantity(entity.getFreeQuantity());
        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setMrp(entity.getMrp());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setPurchasePricePerUnit(entity.getPurchasePricePerUnit());
        dto.setMrpPerUnit(entity.getMrpPerUnit());
        dto.setSellingPricePerUnit(entity.getSellingPricePerUnit());
        dto.setRackLocation(entity.getRackLocation());
        return dto;
    }

    private PackagingDetailsDto toDto(PackagingDetails entity) {
        if (entity == null) return null;
        PackagingDetailsDto dto = new PackagingDetailsDto();
        dto.setPackagingId(entity.getPackagingId());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setPurchaseUnitContains(entity.getPurchaseUnitContains());
        dto.setSmallestUnit(entity.getSmallestUnit());
        return dto;
    }

    private ProductAttributeSupplementsDto toDto(ProductAttributeSupplements entity) {
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
        dto.setNetQuantityUnit(entity.getNetQuantityUnit());
        dto.setGender(entity.getGender());
        dto.setManufacturerName(entity.getManufacturerName());
        dto.setFssaiLicenseNumber(entity.getFssaiLicenseNumber());
        return dto;
    }

    private ProductAttributeCosmeticsDto toDto(ProductAttributeCosmetics entity) {
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
        if (entity.getNetQuantityUnit() != null) dto.setNetQuantityUnitId(entity.getNetQuantityUnit().getNetQuantityUnitId());
        dto.setManufacturerName(entity.getManufacturerName());
        
        return dto;
    }

    private List<ProductAttributeDrugDto> toDrugDtoList(List<ProductAttributeDrug> entities) {
        if (entities == null || entities.isEmpty()) return new ArrayList<>();
        
        List<ProductAttributeDrugDto> dtos = new ArrayList<>();
        for (ProductAttributeDrug drugEntity : entities) {
            ProductAttributeDrugDto dto = new ProductAttributeDrugDto();
            dto.setDrugSchedule(drugEntity.getDrugSchedule());
            
            if (drugEntity.getProductMolecules() != null) {
                List<ProductMoleculeDto> moleculeDtos = new ArrayList<>();
                for (ProductMolecule molEntity : drugEntity.getProductMolecules()) {
                    ProductMoleculeDto molDto = new ProductMoleculeDto();
                    if (molEntity.getId() != null) {
                        molDto.setProductAttributeId(molEntity.getId().getProductAttributeId());
                    }
                    molDto.setMoleculeStrength(molEntity.getMoleculeStrength());
                    if (molEntity.getMolecule() != null) {
                        molDto.setMoleculeId(molEntity.getMolecule().getMoleculeId());
                    }
                    moleculeDtos.add(molDto);
                }
                dto.setProductMolecules(moleculeDtos);
            }
            dtos.add(dto);
        }
        return dtos;
    }
}
