package tiameds.pharmabackend.mapper.product;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.PharmaBatchDetailsDto;
import tiameds.pharmabackend.dto.product.PharmaPackagingDetailsDto;
import tiameds.pharmabackend.dto.product.PharmaProductAttributeSupplementsDto;
import tiameds.pharmabackend.dto.product.PharmaProductDetailsDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.DosageForm;
import tiameds.pharmabackend.entity.master.Flavour;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;
import tiameds.pharmabackend.entity.product.PharmaBatchDetails;
import tiameds.pharmabackend.entity.product.PharmaPackagingDetails;
import tiameds.pharmabackend.entity.product.PharmaProductAttributeSupplements;
import tiameds.pharmabackend.entity.product.PharmaProductDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;
import java.util.ArrayList;
import tiameds.pharmabackend.entity.product.PharmaProductAttributeDrug;
import tiameds.pharmabackend.dto.product.PharmaProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.PharmaProductMoleculeDto;
import tiameds.pharmabackend.entity.product.PharmaProductMolecule;
import tiameds.pharmabackend.entity.master.Molecule;
import tiameds.pharmabackend.entity.PharmacyDetails;
@Component
public class PharmaProductMapper {

    public PharmaProductDetails toEntity(PharmaProductDetailsDto dto, String generatedProductId, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        
        PharmaProductDetails entity = new PharmaProductDetails();
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
            List<PharmaBatchDetails> batchList = dto.getBatchDetails().stream().map(b -> toEntity(b, createdBy, createdAt)).collect(Collectors.toList());
            batchList.forEach(b -> b.setProduct(entity));
            entity.setBatchDetails(batchList);
        }
        
        if (dto.getPackagingDetails() != null) {
            List<PharmaPackagingDetails> packList = dto.getPackagingDetails().stream().map(p -> toEntity(p, createdBy, createdAt)).collect(Collectors.toList());
            packList.forEach(p -> p.setProduct(entity));
            entity.setPackagingDetails(packList);
        }
        
        if (dto.getProductAttributeSupplements() != null) {
            List<PharmaProductAttributeSupplements> suppList = dto.getProductAttributeSupplements().stream().map(s -> toEntity(s, createdBy, createdAt)).collect(Collectors.toList());
            suppList.forEach(s -> s.setProduct(entity));
            entity.setProductAttributeSupplements(suppList);
        }
        
        if (dto.getProductAttributeDrugs() != null) {
            List<PharmaProductAttributeDrug> drugList = new ArrayList<>();
            for (PharmaProductAttributeDrugDto dDto : dto.getProductAttributeDrugs()) {
                PharmaProductAttributeDrug drugEntity = new PharmaProductAttributeDrug();
                drugEntity.setDrugSchedule(dDto.getDrugSchedule());
                drugEntity.setCreatedBy(createdBy);
                drugEntity.setCreatedAt(createdAt);
                
                if (dDto.getProductMolecules() != null) {
                    List<PharmaProductMolecule> molList = new ArrayList<>();
                    for (PharmaProductMoleculeDto mDto : dDto.getProductMolecules()) {
                        PharmaProductMolecule mol = new PharmaProductMolecule();
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

    private PharmaBatchDetails toEntity(PharmaBatchDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PharmaBatchDetails entity = new PharmaBatchDetails();
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

    private PharmaPackagingDetails toEntity(PharmaPackagingDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PharmaPackagingDetails entity = new PharmaPackagingDetails();
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setPurchaseUnitContains(dto.getPurchaseUnitContains());
        entity.setSmallestUnit(dto.getSmallestUnit());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private PharmaProductAttributeSupplements toEntity(PharmaProductAttributeSupplementsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PharmaProductAttributeSupplements entity = new PharmaProductAttributeSupplements();
        
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

    public PharmaProductDetailsDto toDto(PharmaProductDetails entity) {
        if (entity == null) return null;
        PharmaProductDetailsDto dto = new PharmaProductDetailsDto();
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
        
        if (entity.getProductAttributeDrugs() != null && !entity.getProductAttributeDrugs().isEmpty()) {
            dto.setProductAttributeDrugs(toDrugDtoList(entity.getProductAttributeDrugs()));
        }
        
        return dto;
    }

    private PharmaBatchDetailsDto toDto(PharmaBatchDetails entity) {
        if (entity == null) return null;
        PharmaBatchDetailsDto dto = new PharmaBatchDetailsDto();
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

    private PharmaPackagingDetailsDto toDto(PharmaPackagingDetails entity) {
        if (entity == null) return null;
        PharmaPackagingDetailsDto dto = new PharmaPackagingDetailsDto();
        dto.setPackagingId(entity.getPackagingId());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setPurchaseUnitContains(entity.getPurchaseUnitContains());
        dto.setSmallestUnit(entity.getSmallestUnit());
        return dto;
    }

    private PharmaProductAttributeSupplementsDto toDto(PharmaProductAttributeSupplements entity) {
        if (entity == null) return null;
        PharmaProductAttributeSupplementsDto dto = new PharmaProductAttributeSupplementsDto();
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

    private List<PharmaProductAttributeDrugDto> toDrugDtoList(List<PharmaProductAttributeDrug> entities) {
        if (entities == null || entities.isEmpty()) return new ArrayList<>();
        
        List<PharmaProductAttributeDrugDto> dtos = new ArrayList<>();
        for (PharmaProductAttributeDrug drugEntity : entities) {
            PharmaProductAttributeDrugDto dto = new PharmaProductAttributeDrugDto();
            dto.setDrugSchedule(drugEntity.getDrugSchedule());
            
            if (drugEntity.getProductMolecules() != null) {
                List<PharmaProductMoleculeDto> moleculeDtos = new ArrayList<>();
                for (PharmaProductMolecule molEntity : drugEntity.getProductMolecules()) {
                    PharmaProductMoleculeDto molDto = new PharmaProductMoleculeDto();
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
