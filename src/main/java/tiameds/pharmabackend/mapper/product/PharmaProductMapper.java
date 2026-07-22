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

@Component
public class PharmaProductMapper {

    public PharmaProductDetails toEntity(PharmaProductDetailsDto dto, String generatedProductId) {
        if (dto == null) return null;
        
        PharmaProductDetails entity = new PharmaProductDetails();
        entity.setProductId(generatedProductId);
        
        if (dto.getPharmacyId() != null) {
            tiameds.pharmabackend.entity.PharmacyDetails pharmacy = new tiameds.pharmabackend.entity.PharmacyDetails();
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
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        
        if (dto.getBatchDetails() != null) {
            java.util.List<PharmaBatchDetails> batchList = dto.getBatchDetails().stream().map(this::toEntity).collect(Collectors.toList());
            batchList.forEach(b -> b.setProduct(entity));
            entity.setBatchDetails(batchList);
        }
        
        if (dto.getPackagingDetails() != null) {
            java.util.List<PharmaPackagingDetails> packList = dto.getPackagingDetails().stream().map(this::toEntity).collect(Collectors.toList());
            packList.forEach(p -> p.setProduct(entity));
            entity.setPackagingDetails(packList);
        }
        
        if (dto.getProductAttributeSupplements() != null) {
            java.util.List<PharmaProductAttributeSupplements> suppList = dto.getProductAttributeSupplements().stream().map(this::toEntity).collect(Collectors.toList());
            suppList.forEach(s -> s.setProduct(entity));
            entity.setProductAttributeSupplements(suppList);
        }
        
        return entity;
    }

    private PharmaBatchDetails toEntity(PharmaBatchDetailsDto dto) {
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
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    private PharmaPackagingDetails toEntity(PharmaPackagingDetailsDto dto) {
        if (dto == null) return null;
        PharmaPackagingDetails entity = new PharmaPackagingDetails();
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setPurchaseUnitContains(dto.getPurchaseUnitContains());
        entity.setSmallestUnit(dto.getSmallestUnit());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    private PharmaProductAttributeSupplements toEntity(PharmaProductAttributeSupplementsDto dto) {
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
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setManufacturerName(dto.getManufacturerName());
        entity.setFssaiLicenseNumber(dto.getFssaiLicenseNumber());
        
        return entity;
    }
}
