package tiameds.pharmabackend.service.product.bulk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.entity.master.MaterialType;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.entity.master.PurchaseSmallestUnit;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;
import tiameds.pharmabackend.repository.master.*;

/**
 * Builds the {@link MasterIndex} snapshot used by one bulk upload.
 * <p>
 * Runs inside a read-only transaction so the lazy parent associations
 * (a sub-type's type, a device sub-category's category) can be read while indexing.
 */
@Service
@RequiredArgsConstructor
public class MasterIndexLoader {

    private final ProductCategoryRepository productCategoryRepo;
    private final PurchaseSmallestUnitRepository purchaseSmallestUnitRepo;
    private final DosageFormRepository dosageFormRepo;
    private final MoleculeRepository moleculeRepo;
    private final TherapeuticCategoryRepository therapeuticCategoryRepo;
    private final TherapeuticSubcategoryRepository therapeuticSubcategoryRepo;
    private final FlavourRepository flavourRepo;
    private final AgeGroupRepository ageGroupRepo;
    private final CountryRepository countryRepo;
    private final SkinTypeRepository skinTypeRepo;
    private final HairTypeRepository hairTypeRepo;
    private final IntendedUseAreaRepository intendedUseAreaRepo;
    private final PowerSourceRepository powerSourceRepo;
    private final NetQuantityUnitRepository netQuantityUnitRepo;
    private final MaterialTypeRepository materialTypeRepo;
    private final ProductTypeRepository productTypeRepo;
    private final ProductSubTypeRepository productSubTypeRepo;
    private final ProductFormRepository productFormRepo;
    private final DeviceCategoryRepository deviceCategoryRepo;
    private final DeviceSubCategoryRepository deviceSubCategoryRepo;
    private final DeviceSpecificationUnitRepository deviceSpecificationUnitRepo;

    @Transactional(readOnly = true)
    public MasterIndex load() {
        MasterIndex index = new MasterIndex();

        index.put(MasterIndex.PRODUCT_CATEGORY, productCategoryRepo.findAll(),
                c -> c.getProductCategoryName(), c -> c.getProductCategoryId());
        index.put(MasterIndex.DOSAGE_FORM, dosageFormRepo.findAll(),
                d -> d.getDosageName(), d -> d.getDosageId());
        index.put(MasterIndex.MOLECULE, moleculeRepo.findAll(),
                m -> m.getMoleculeName(), m -> m.getMoleculeId());
        index.put(MasterIndex.THERAPEUTIC_CATEGORY, therapeuticCategoryRepo.findAll(),
                t -> t.getTherapeuticCategoryName(), t -> t.getTherapeuticCategoryId());
        index.put(MasterIndex.FLAVOUR, flavourRepo.findAll(),
                f -> f.getFlavourName(), f -> f.getFlavourId());
        index.put(MasterIndex.AGE_GROUP, ageGroupRepo.findAll(),
                a -> a.getAgeGroupName(), a -> a.getAgeGroupId());
        index.put(MasterIndex.COUNTRY, countryRepo.findAll(),
                c -> c.getCountryName(), c -> c.getCountryId());
        index.put(MasterIndex.SKIN_TYPE, skinTypeRepo.findAll(),
                s -> s.getSkinTypeName(), s -> s.getSkinTypeId());
        index.put(MasterIndex.HAIR_TYPE, hairTypeRepo.findAll(),
                h -> h.getHairTypeName(), h -> h.getHairTypeId());
        index.put(MasterIndex.INTENDED_USE_AREA, intendedUseAreaRepo.findAll(),
                i -> i.getIntendedUseAreaName(), i -> i.getIntendedUseAreaId());
        index.put(MasterIndex.POWER_SOURCE, powerSourceRepo.findAll(),
                p -> p.getPowerSourceName(), p -> p.getPowerSourceId());

        index.putUnitPair(purchaseSmallestUnitRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                PurchaseSmallestUnit::getPurchaseUnitName,
                PurchaseSmallestUnit::getPurchaseSmallestUnitName,
                PurchaseSmallestUnit::getPurchaseSmallestUnitId);

        index.putScoped(MasterIndex.NET_QUANTITY_UNIT, netQuantityUnitRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                NetQuantityUnit::getNetQuantityUnitName, NetQuantityUnit::getNetQuantityUnitId);
        index.putScoped(MasterIndex.MATERIAL_TYPE, materialTypeRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                MaterialType::getMaterialTypeName, MaterialType::getMaterialTypeId);
        index.putScoped(MasterIndex.PRODUCT_TYPE, productTypeRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                ProductType::getProductTypeName, ProductType::getProductTypeId);
        index.putScoped(MasterIndex.PRODUCT_FORM, productFormRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                ProductForm::getProductFormName, ProductForm::getProductFormId);
        index.putScoped(MasterIndex.DEVICE_CATEGORY, deviceCategoryRepo.findAll(),
                MasterIndexLoader::categoryIdOf,
                DeviceCategory::getDeviceCategoryName, DeviceCategory::getDeviceCategoryId);

        index.putScoped(MasterIndex.PRODUCT_SUB_TYPE, productSubTypeRepo.findAll(),
                s -> s.getProductType() == null ? null : s.getProductType().getProductTypeId(),
                ProductSubType::getProductSubTypeName, ProductSubType::getProductSubTypeId);
        index.putScoped(MasterIndex.THERAPEUTIC_SUBCATEGORY, therapeuticSubcategoryRepo.findAll(),
                s -> s.getTherapeuticCategory() == null
                        ? null : s.getTherapeuticCategory().getTherapeuticCategoryId(),
                TherapeuticSubcategory::getTherapeuticSubcategoryName,
                TherapeuticSubcategory::getTherapeuticSubcategoryId);
        index.putScoped(MasterIndex.DEVICE_SUB_CATEGORY, deviceSubCategoryRepo.findAll(),
                s -> s.getDeviceCategory() == null ? null : s.getDeviceCategory().getDeviceCategoryId(),
                DeviceSubCategory::getDeviceSubCategoryName, DeviceSubCategory::getDeviceSubCategoryId);
        index.putScoped(MasterIndex.DEVICE_SPECIFICATION_UNIT, deviceSpecificationUnitRepo.findAll(),
                u -> u.getDeviceSubCategory() == null
                        ? null : u.getDeviceSubCategory().getDeviceSubCategoryId(),
                DeviceSpecificationUnit::getDeviceSpecificationUnitName,
                DeviceSpecificationUnit::getDeviceSpecificationUnitId);

        return index;
    }

    private static Long categoryIdOf(PurchaseSmallestUnit row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }

    private static Long categoryIdOf(NetQuantityUnit row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }

    private static Long categoryIdOf(MaterialType row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }

    private static Long categoryIdOf(ProductType row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }

    private static Long categoryIdOf(ProductForm row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }

    private static Long categoryIdOf(DeviceCategory row) {
        return row.getProductCategory() == null ? null : row.getProductCategory().getProductCategoryId();
    }
}
