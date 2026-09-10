package tiameds.pharmabackend.mapper.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.ProductMoleculeDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.master.Molecule;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductAttributeConsumableMedical;
import tiameds.pharmabackend.entity.product.ProductAttributeCosmetics;
import tiameds.pharmabackend.entity.product.ProductAttributeDrug;
import tiameds.pharmabackend.entity.product.ProductAttributeFoodInfant;
import tiameds.pharmabackend.entity.product.ProductAttributeNonConsumableMedical;
import tiameds.pharmabackend.entity.product.ProductAttributeSupplements;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.product.ProductMolecule;
import tiameds.pharmabackend.mapper.product.category.ConsumableMapper;
import tiameds.pharmabackend.mapper.product.category.CosmeticMapper;
import tiameds.pharmabackend.mapper.product.category.DrugMapper;
import tiameds.pharmabackend.mapper.product.category.FoodInfantMapper;
import tiameds.pharmabackend.mapper.product.category.ProductInventoryMapper;
import tiameds.pharmabackend.mapper.product.category.NonConsumableMapper;
import tiameds.pharmabackend.mapper.product.category.SupplementMapper;

@Component
public class ProductMapper {

    @Autowired
    private DrugMapper drugMapper;
    
    @Autowired
    private CosmeticMapper cosmeticMapper;
    
    @Autowired
    private FoodInfantMapper foodInfantMapper;
    
    @Autowired
    private SupplementMapper supplementMapper;
    
    @Autowired
    private ConsumableMapper consumableMapper;
    
    @Autowired
    private NonConsumableMapper nonConsumableMapper;
    
    @Autowired
    private ProductInventoryMapper inventoryMapper;


    public ProductDetails toEntity(ProductDetailsDto dto, String generatedProductId, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        
        ProductDetails entity = new ProductDetails();
        entity.setProductId(generatedProductId);
        
//        if (dto.getPharmacyId() != null) {
//            PharmacyDetails pharmacy = new PharmacyDetails();
//            pharmacy.setPharmacyId(dto.getPharmacyId());
//            entity.setPharmacy(pharmacy);
//        }
        
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
            List<BatchDetails> batchList = dto.getBatchDetails().stream().map(b -> inventoryMapper.toEntity(b, createdBy, createdAt)).collect(Collectors.toList());
            batchList.forEach(b -> b.setProduct(entity));
            entity.setBatchDetails(batchList);
        }
        
        if (dto.getPackagingDetails() != null) {
            List<PackagingDetails> packList = dto.getPackagingDetails().stream().map(p -> inventoryMapper.toEntity(p, createdBy, createdAt)).collect(Collectors.toList());
            packList.forEach(p -> p.setProduct(entity));
            entity.setPackagingDetails(packList);
        }
        
        if (dto.getProductAttributeSupplements() != null) {
            List<ProductAttributeSupplements> suppList = dto.getProductAttributeSupplements().stream().map(s -> supplementMapper.toEntity(s, createdBy, createdAt)).collect(Collectors.toList());
            suppList.forEach(s -> s.setProduct(entity));
            entity.setProductAttributeSupplements(suppList);
        }
        
        if (dto.getProductAttributeCosmetics() != null) {
            List<ProductAttributeCosmetics> cosmList = dto.getProductAttributeCosmetics().stream().map(c -> cosmeticMapper.toEntity(c, createdBy, createdAt)).collect(Collectors.toList());
            cosmList.forEach(c -> c.setProduct(entity));
            entity.setProductAttributeCosmetics(cosmList);
        }
        
        if (dto.getProductAttributeFoodInfants() != null) {
            List<ProductAttributeFoodInfant> foodList = dto.getProductAttributeFoodInfants().stream().map(f -> foodInfantMapper.toEntity(f, createdBy, createdAt)).collect(Collectors.toList());
            foodList.forEach(f -> f.setProduct(entity));
            entity.setProductAttributeFoodInfants(foodList);
        }
        

        if (dto.getProductAttributeConsumableMedicals() != null) {
            List<ProductAttributeConsumableMedical> consList = dto.getProductAttributeConsumableMedicals().stream().map(c -> consumableMapper.toEntity(c, createdBy, createdAt)).collect(Collectors.toList());
            consList.forEach(c -> c.setProduct(entity));
            entity.setProductAttributeConsumableMedicals(consList);
        }
        
        if (dto.getProductAttributeNonConsumableMedicals() != null) {
            List<ProductAttributeNonConsumableMedical> nconsList = dto.getProductAttributeNonConsumableMedicals().stream().map(nc -> nonConsumableMapper.toEntity(nc, createdBy, createdAt)).collect(Collectors.toList());
            nconsList.forEach(nc -> nc.setProduct(entity));
            entity.setProductAttributeNonConsumableMedicals(nconsList);
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


    /**
     * Partial update of a product's attribute lists.
     * <p>
     * Only the lists that are present (non-null) on the dto are rebuilt; a null list
     * means "leave that attribute type exactly as it is". A present-but-empty list
     * clears the attribute type.
     * <p>
     * The attribute ids are deterministic per product (same suffixes as onboarding),
     * so the caller must have cleared and flushed the lists being replaced before
     * calling this — otherwise the re-inserted rows collide with the rows still
     * pending deletion.
     */
    public void applyAttributeUpdates(ProductDetails product, ProductDetailsDto dto,
                                      String modifiedBy, LocalDateTime modifiedAt) {
        if (product == null || dto == null) return;
        String productId = product.getProductId();

        if (dto.getProductAttributeSupplements() != null) {
            List<ProductAttributeSupplements> list = new ArrayList<>();
            for (var sDto : dto.getProductAttributeSupplements()) {
                ProductAttributeSupplements e = supplementMapper.toEntity(sDto, modifiedBy, modifiedAt);
                e.setProductAttributeId(productId + "_SUPP");
                e.setProduct(product);
                e.setModifiedBy(modifiedBy);
                e.setModifiedAt(modifiedAt);
                sDto.setProductAttributeId(e.getProductAttributeId());
                list.add(e);
            }
            product.setProductAttributeSupplements(appendInto(product.getProductAttributeSupplements(), list));
        }

        if (dto.getProductAttributeCosmetics() != null) {
            List<ProductAttributeCosmetics> list = new ArrayList<>();
            for (var cDto : dto.getProductAttributeCosmetics()) {
                ProductAttributeCosmetics e = cosmeticMapper.toEntity(cDto, modifiedBy, modifiedAt);
                e.setProductAttributeId(productId + "_COSM");
                e.setProduct(product);
                e.setModifiedBy(modifiedBy);
                e.setModifiedAt(modifiedAt);
                cDto.setProductAttributeId(e.getProductAttributeId());
                list.add(e);
            }
            product.setProductAttributeCosmetics(appendInto(product.getProductAttributeCosmetics(), list));
        }

        if (dto.getProductAttributeFoodInfants() != null) {
            List<ProductAttributeFoodInfant> list = new ArrayList<>();
            for (var fDto : dto.getProductAttributeFoodInfants()) {
                ProductAttributeFoodInfant e = foodInfantMapper.toEntity(fDto, modifiedBy, modifiedAt);
                e.setProductAttributeId(productId + "_FOOD");
                e.setProduct(product);
                e.setModifiedBy(modifiedBy);
                e.setModifiedAt(modifiedAt);
                fDto.setProductAttributeId(e.getProductAttributeId());
                list.add(e);
            }
            product.setProductAttributeFoodInfants(appendInto(product.getProductAttributeFoodInfants(), list));
        }

        if (dto.getProductAttributeConsumableMedicals() != null) {
            List<ProductAttributeConsumableMedical> list = new ArrayList<>();
            for (var cDto : dto.getProductAttributeConsumableMedicals()) {
                ProductAttributeConsumableMedical e = consumableMapper.toEntity(cDto, modifiedBy, modifiedAt);
                e.setProductAttributeId(productId + "_CONS");
                e.setProduct(product);
                e.setModifiedBy(modifiedBy);
                e.setModifiedAt(modifiedAt);
                cDto.setProductAttributeId(e.getProductAttributeId());
                list.add(e);
            }
            product.setProductAttributeConsumableMedicals(
                    appendInto(product.getProductAttributeConsumableMedicals(), list));
        }

        if (dto.getProductAttributeNonConsumableMedicals() != null) {
            List<ProductAttributeNonConsumableMedical> list = new ArrayList<>();
            for (var ncDto : dto.getProductAttributeNonConsumableMedicals()) {
                ProductAttributeNonConsumableMedical e = nonConsumableMapper.toEntity(ncDto, modifiedBy, modifiedAt);
                e.setProductAttributeId(productId + "_NCONS");
                e.setProduct(product);
                e.setModifiedBy(modifiedBy);
                e.setModifiedAt(modifiedAt);
                ncDto.setProductAttributeId(e.getProductAttributeId());
                list.add(e);
            }
            product.setProductAttributeNonConsumableMedicals(
                    appendInto(product.getProductAttributeNonConsumableMedicals(), list));
        }

        if (dto.getProductAttributeDrugs() != null) {
            List<ProductAttributeDrug> list = new ArrayList<>();
            int index = 0;
            for (ProductAttributeDrugDto dDto : dto.getProductAttributeDrugs()) {
                String drugAttrId = productId + "_DRUG_" + (++index);

                ProductAttributeDrug drugEntity = new ProductAttributeDrug();
                drugEntity.setProductAttributeId(drugAttrId);
                drugEntity.setProduct(product);
                drugEntity.setDrugSchedule(dDto.getDrugSchedule());
                drugEntity.setCreatedBy(modifiedBy);
                drugEntity.setCreatedAt(modifiedAt);
                drugEntity.setModifiedBy(modifiedBy);
                drugEntity.setModifiedAt(modifiedAt);

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
                        tiameds.pharmabackend.entity.product.ProductMoleculeId molId =
                                new tiameds.pharmabackend.entity.product.ProductMoleculeId();
                        molId.setProductAttributeId(drugAttrId);
                        molId.setMoleculeId(mDto.getMoleculeId());
                        mol.setId(molId);
                        mol.setProductAttributeDrug(drugEntity);
                        molList.add(mol);
                        mDto.setProductAttributeId(drugAttrId);
                    }
                    drugEntity.setProductMolecules(molList);
                }

                dDto.setProductAttributeId(drugAttrId);
                list.add(drugEntity);
            }
            product.setProductAttributeDrugs(appendInto(product.getProductAttributeDrugs(), list));
        }
    }

    /**
     * Adds the replacement rows into the product's own (orphan-removal managed)
     * collection instead of swapping the reference, which Hibernate rejects.
     */
    private <E> List<E> appendInto(List<E> current, List<E> replacement) {
        if (current == null) return new ArrayList<>(replacement);
        current.addAll(replacement);
        return current;
    }


    public ProductDetailsDto toDto(ProductDetails entity) {
        if (entity == null) return null;
        ProductDetailsDto dto = new ProductDetailsDto();
        dto.setProductId(entity.getProductId());
        
        // OLD single-pharmacy DTO mapping:
        // if (entity.getPharmacy() != null) {
        //     dto.setPharmacyId(entity.getPharmacy().getPharmacyId());
        // }
        if (entity.getPharmacies() != null && !entity.getPharmacies().isEmpty()) {
            dto.setPharmacyId(entity.getPharmacies().get(0).getPharmacyId());
        }
        if (entity.getProductCategory() != null) {
            dto.setProductCategoryId(entity.getProductCategory().getProductCategoryId());
        }
        
        dto.setProductName(entity.getProductName());
        dto.setBrandName(entity.getBrandName());
        dto.setGstPercentage(entity.getGstPercentage());
        dto.setHsnNo(entity.getHsnNo());
        
        if (entity.getBatchDetails() != null) {
            dto.setBatchDetails(entity.getBatchDetails().stream().map(inventoryMapper::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getPackagingDetails() != null) {
            dto.setPackagingDetails(entity.getPackagingDetails().stream().map(inventoryMapper::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeSupplements() != null) {
            dto.setProductAttributeSupplements(entity.getProductAttributeSupplements().stream().map(supplementMapper::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeCosmetics() != null) {
            dto.setProductAttributeCosmetics(entity.getProductAttributeCosmetics().stream().map(cosmeticMapper::toDto).collect(Collectors.toList()));
        }
        
        if (entity.getProductAttributeFoodInfants() != null) {
            dto.setProductAttributeFoodInfants(entity.getProductAttributeFoodInfants().stream().map(foodInfantMapper::toDto).collect(Collectors.toList()));
        }
        

        if (entity.getProductAttributeConsumableMedicals() != null) {
            dto.setProductAttributeConsumableMedicals(entity.getProductAttributeConsumableMedicals().stream().map(consumableMapper::toDto).collect(Collectors.toList()));
        }
        if (entity.getProductAttributeNonConsumableMedicals() != null) {
            dto.setProductAttributeNonConsumableMedicals(entity.getProductAttributeNonConsumableMedicals().stream().map(nonConsumableMapper::toDto).collect(Collectors.toList()));
        }

        if (entity.getProductAttributeDrugs() != null && !entity.getProductAttributeDrugs().isEmpty()) {
            dto.setProductAttributeDrugs(drugMapper.toDrugDtoList(entity.getProductAttributeDrugs()));
        }
        
        return dto;
    }


}
