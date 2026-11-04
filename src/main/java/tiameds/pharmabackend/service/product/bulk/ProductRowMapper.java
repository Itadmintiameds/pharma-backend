package tiameds.pharmabackend.service.product.bulk;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeConsumableMedicalDto;
import tiameds.pharmabackend.dto.product.ProductAttributeCosmeticsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductAttributeFoodInfantDto;
import tiameds.pharmabackend.dto.product.ProductAttributeNonConsumableMedicalDto;
import tiameds.pharmabackend.dto.product.ProductAttributeSupplementsDto;
import tiameds.pharmabackend.dto.product.ProductMoleculeDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static tiameds.pharmabackend.service.product.bulk.ProductBulkColumns.*;

/**
 * Turns one CSV line into the product/packaging/batch/attribute DTOs.
 * <p>
 * Two kinds of problem are treated differently. A structural one — no product name,
 * no recognised category, no packaging unit, prices that are not numbers — fails the
 * line, because the row cannot be written without it. Anything else degrades to a
 * warning on a line that still succeeds: a master name the database does not know,
 * or a column the product's category has nowhere to store. Warnings surface in the
 * response so an operator can see exactly what the import did not keep.
 */
@Component
public class ProductRowMapper {

    /**
     * Columns every category uses; only what falls outside these and the category's
     * own columns is reported as unstored.
     */
    private static final Set<String> COMMON_COLUMNS = Set.of(
            PRODUCT_CODE, PRODUCT_NAME, BRAND_NAME, CATEGORY, HSN_CODE, GST_PERCENT,
            PURCHASE_UNIT, SMALLEST_UNIT, UNITS_PER_PURCHASE_UNIT,
            BATCH_NUMBER, MANUFACTURING_DATE, EXPIRY_DATE, PURCHASE_PRICE_PER_UNIT,
            MRP_PER_PACK, SELLING_PRICE_PER_UNIT, STOCK_IN_SMALLEST_UNITS, RACK_LOCATION);

    /**
     * Maps a line. Throws {@link RowRejectedException} when the line cannot be written.
     */
    public MappedRow map(CsvFile.Row row, MasterIndex masters) {
        MappedRow mapped = new MappedRow();
        mapped.setLine(row.getLineNumber());
        mapped.setProductCode(CellCoercion.text(row, PRODUCT_CODE));

        mapProduct(row, masters, mapped);
        mapPackaging(row, masters, mapped);
        mapBatch(row, mapped);

        Set<String> used = new LinkedHashSet<>(COMMON_COLUMNS);
        mapAttributes(row, masters, mapped, used);
        warnAboutUnstoredColumns(row, mapped, used);

        return mapped;
    }

    // ===== product =====

    private void mapProduct(CsvFile.Row row, MasterIndex masters, MappedRow mapped) {
        String name = CellCoercion.text(row, PRODUCT_NAME);
        if (name == null) {
            throw new RowRejectedException("Column '" + PRODUCT_NAME + "' is required");
        }
        mapped.setProductName(name);
        mapped.setBrandName(CellCoercion.text(row, BRAND_NAME));
        mapped.setHsnNo(CellCoercion.code(row, HSN_CODE));
        mapped.setGstPercentage(CellCoercion.code(row, GST_PERCENT));

        String categoryName = CellCoercion.text(row, CATEGORY);
        if (categoryName == null) {
            throw new RowRejectedException("Column '" + CATEGORY + "' is required");
        }
        Long categoryId = masters.find(MasterIndex.PRODUCT_CATEGORY, categoryName);
        if (categoryId == null) {
            throw new RowRejectedException(
                    "Unknown product category \"" + categoryName + "\" — add it to the category master first");
        }
        mapped.setCategoryName(categoryName);
        mapped.setProductCategoryId(categoryId);
        mapped.setCategoryKind(kindOf(categoryName));
    }

    /**
     * Routes a category name to its attribute table by keyword rather than exact
     * match, so master rows can be renamed without breaking imports. Non-consumable
     * is tested before consumable because it contains it.
     */
    private MappedRow.CategoryKind kindOf(String categoryName) {
        String n = categoryName.toLowerCase();
        if (n.contains("non-consumable") || n.contains("non consumable")) {
            return MappedRow.CategoryKind.NON_CONSUMABLE_DEVICE;
        }
        if (n.contains("consumable")) {
            return MappedRow.CategoryKind.CONSUMABLE_DEVICE;
        }
        if (n.contains("cosmetic") || n.contains("personal care")) {
            return MappedRow.CategoryKind.COSMETIC;
        }
        if (n.contains("supplement") || n.contains("nutraceutical")) {
            return MappedRow.CategoryKind.SUPPLEMENT;
        }
        if (n.contains("infant") || n.contains("food")) {
            return MappedRow.CategoryKind.FOOD_INFANT;
        }
        if (n.contains("drug") || n.contains("medicine") || n.contains("pharma")) {
            return MappedRow.CategoryKind.DRUG;
        }
        return MappedRow.CategoryKind.NONE;
    }

    // ===== packaging =====

    private void mapPackaging(CsvFile.Row row, MasterIndex masters, MappedRow mapped) {
        String purchaseUnit = CellCoercion.text(row, PURCHASE_UNIT);
        String smallestUnit = CellCoercion.text(row, SMALLEST_UNIT);
        if (purchaseUnit == null || smallestUnit == null) {
            throw new RowRejectedException(
                    "Columns '" + PURCHASE_UNIT + "' and '" + SMALLEST_UNIT + "' are both required");
        }

        Long unitId = masters.findUnitPair(mapped.getProductCategoryId(), purchaseUnit, smallestUnit);
        if (unitId == null) {
            throw new RowRejectedException("Unknown unit combination \"" + purchaseUnit + "\" of \""
                    + smallestUnit + "\" for category \"" + mapped.getCategoryName()
                    + "\" — add it to the purchase/smallest unit master first");
        }
        mapped.setPurchaseSmallestUnitId(unitId);
        mapped.setPurchaseUnitName(purchaseUnit);
        mapped.setSmallestUnitName(smallestUnit);

        Long contains = CellCoercion.integer(row, UNITS_PER_PURCHASE_UNIT);
        if (contains == null || contains < 1) {
            throw new RowRejectedException(
                    "Column '" + UNITS_PER_PURCHASE_UNIT + "' must be a whole number of 1 or more");
        }
        mapped.setPurchaseUnitContains(contains);
    }

    // ===== batch =====

    private void mapBatch(CsvFile.Row row, MappedRow mapped) {
        String batchNumber = CellCoercion.text(row, BATCH_NUMBER);
        if (batchNumber == null) {
            throw new RowRejectedException("Column '" + BATCH_NUMBER + "' is required");
        }

        BatchDetailsDto batch = new BatchDetailsDto();
        batch.setBatchNumber(batchNumber);
        batch.setPurchaseUnit(mapped.getPurchaseUnitName());
        batch.setRackLocation(CellCoercion.text(row, RACK_LOCATION));
        batch.setManufacturingDate(CellCoercion.date(row, MANUFACTURING_DATE));
        batch.setExpiryDate(CellCoercion.date(row, EXPIRY_DATE));

        if (batch.getManufacturingDate() != null && batch.getExpiryDate() != null
                && batch.getExpiryDate().isBefore(batch.getManufacturingDate())) {
            throw new RowRejectedException("Expiry date is before the manufacturing date");
        }

        // The template quotes per-unit purchase/selling prices and a per-pack MRP.
        // Both halves are stored, so the missing half of each pair is derived from
        // the pack size rather than left null for downstream billing to guess at.
        long contains = mapped.getPurchaseUnitContains();

        Double purchasePerUnit = CellCoercion.decimal(row, PURCHASE_PRICE_PER_UNIT);
        batch.setPurchasePricePerUnit(purchasePerUnit);
        batch.setPurchasePrice(scale(multiply(purchasePerUnit, contains)));

        Double mrpPerPack = CellCoercion.decimal(row, MRP_PER_PACK);
        batch.setMrp(mrpPerPack);
        batch.setMrpPerUnit(scale(divide(mrpPerPack, contains)));

        Double sellingPerUnit = CellCoercion.decimal(row, SELLING_PRICE_PER_UNIT);
        batch.setSellingPricePerUnit(sellingPerUnit);
        batch.setSellingPrice(scale(multiply(sellingPerUnit, contains)));

        mapped.setBatch(batch);

        Long stock = CellCoercion.integer(row, STOCK_IN_SMALLEST_UNITS);
        if (stock != null && stock < 0) {
            throw new RowRejectedException("Column '" + STOCK_IN_SMALLEST_UNITS + "' cannot be negative");
        }
        mapped.setStockInSmallestUnits(stock);
    }

    private Double multiply(Double perUnit, long contains) {
        return perUnit == null ? null : perUnit * contains;
    }

    private Double divide(Double perPack, long contains) {
        return perPack == null ? null : perPack / contains;
    }

    private Double scale(Double value) {
        return value == null ? null : Math.round(value * 100d) / 100d;
    }

    // ===== category attributes =====

    private void mapAttributes(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        switch (mapped.getCategoryKind()) {
            case DRUG -> mapDrug(row, masters, mapped, used);
            case COSMETIC -> mapCosmetics(row, masters, mapped, used);
            case SUPPLEMENT -> mapSupplements(row, masters, mapped, used);
            case FOOD_INFANT -> mapFoodInfant(row, masters, mapped, used);
            case CONSUMABLE_DEVICE -> mapConsumable(row, masters, mapped, used);
            case NON_CONSUMABLE_DEVICE -> mapNonConsumable(row, masters, mapped, used);
            case NONE -> {
                // Product and batches are still created; every category column is
                // reported as unstored by warnAboutUnstoredColumns.
            }
        }
    }

    private void mapDrug(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.add(DRUG_SCHEDULE);
        used.add(MOLECULES);

        ProductAttributeDrugDto drug = new ProductAttributeDrugDto();
        drug.setDrugSchedule(CellCoercion.text(row, DRUG_SCHEDULE));

        List<ProductMoleculeDto> molecules = new ArrayList<>();
        for (CellCoercion.MoleculeEntry entry : CellCoercion.molecules(row, MOLECULES)) {
            Long moleculeId = masters.find(MasterIndex.MOLECULE, entry.name());
            if (moleculeId == null) {
                mapped.addWarning("Unknown molecule \"" + entry.name() + "\" — not linked");
                continue;
            }
            ProductMoleculeDto molecule = new ProductMoleculeDto();
            molecule.setMoleculeId(moleculeId);
            molecule.setMoleculeStrength(entry.strength());
            molecules.add(molecule);
        }
        drug.setProductMolecules(molecules);
        mapped.setDrug(drug);
    }

    private void mapCosmetics(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.addAll(List.of(PRODUCT_TYPE, PRODUCT_SUB_TYPE, PRODUCT_FORM, VARIANT_NAME,
                INTENDED_USE_AREAS, SKIN_TYPES, HAIR_TYPES, AGE_GROUPS, GENDER, FRAGRANCE,
                NET_QUANTITY, NET_QUANTITY_UNIT, MANUFACTURER_NAME));

        ProductAttributeCosmeticsDto dto = new ProductAttributeCosmeticsDto();
        Long typeId = resolve(masters, MasterIndex.PRODUCT_TYPE, mapped.getProductCategoryId(),
                row, PRODUCT_TYPE, mapped);
        dto.setProductTypeId(typeId);
        dto.setProductSubTypeId(resolve(masters, MasterIndex.PRODUCT_SUB_TYPE, typeId,
                row, PRODUCT_SUB_TYPE, mapped));
        dto.setProductFormId(resolve(masters, MasterIndex.PRODUCT_FORM, mapped.getProductCategoryId(),
                row, PRODUCT_FORM, mapped));
        dto.setVariantName(CellCoercion.text(row, VARIANT_NAME));
        dto.setIntendedUseAreaIds(resolveAll(masters, MasterIndex.INTENDED_USE_AREA, row,
                INTENDED_USE_AREAS, mapped));
        dto.setSkinTypeIds(resolveAll(masters, MasterIndex.SKIN_TYPE, row, SKIN_TYPES, mapped));
        dto.setHairTypeIds(resolveAll(masters, MasterIndex.HAIR_TYPE, row, HAIR_TYPES, mapped));
        dto.setAgeGroupIds(resolveAll(masters, MasterIndex.AGE_GROUP, row, AGE_GROUPS, mapped));
        dto.setGender(CellCoercion.text(row, GENDER));
        dto.setFragrance(CellCoercion.text(row, FRAGRANCE));
        dto.setNetQuantity(CellCoercion.decimal(row, NET_QUANTITY));
        dto.setNetQuantityUnitId(resolve(masters, MasterIndex.NET_QUANTITY_UNIT,
                mapped.getProductCategoryId(), row, NET_QUANTITY_UNIT, mapped));
        dto.setManufacturerName(CellCoercion.text(row, MANUFACTURER_NAME));
        mapped.setCosmetics(dto);
    }

    private void mapSupplements(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.addAll(List.of(THERAPEUTIC_CATEGORY, THERAPEUTIC_SUBCATEGORY, FLAVOUR, DOSAGE_FORM,
                AGE_GROUPS, STRENGTH_COMPOSITION, NET_QUANTITY, NET_QUANTITY_UNIT, GENDER,
                MANUFACTURER_NAME, FSSAI_LICENSE_NUMBER));

        ProductAttributeSupplementsDto dto = new ProductAttributeSupplementsDto();
        Long therapeuticId = resolve(masters, MasterIndex.THERAPEUTIC_CATEGORY, null,
                row, THERAPEUTIC_CATEGORY, mapped);
        dto.setTherapeuticCategoryId(therapeuticId);
        dto.setTherapeuticSubcategoryId(resolve(masters, MasterIndex.THERAPEUTIC_SUBCATEGORY,
                therapeuticId, row, THERAPEUTIC_SUBCATEGORY, mapped));
        dto.setFlavourId(resolve(masters, MasterIndex.FLAVOUR, null, row, FLAVOUR, mapped));
        dto.setDosageFormId(resolve(masters, MasterIndex.DOSAGE_FORM, null, row, DOSAGE_FORM, mapped));
        dto.setAgeGroupIds(resolveAll(masters, MasterIndex.AGE_GROUP, row, AGE_GROUPS, mapped));
        dto.setStrengthComposition(CellCoercion.text(row, STRENGTH_COMPOSITION));
        dto.setNetQuantity(CellCoercion.decimal(row, NET_QUANTITY));
        dto.setNetQuantityUnitId(resolve(masters, MasterIndex.NET_QUANTITY_UNIT,
                mapped.getProductCategoryId(), row, NET_QUANTITY_UNIT, mapped));
        dto.setGender(CellCoercion.text(row, GENDER));
        dto.setManufacturerName(CellCoercion.text(row, MANUFACTURER_NAME));
        dto.setFssaiLicenseNumber(CellCoercion.code(row, FSSAI_LICENSE_NUMBER));
        mapped.setSupplements(dto);
    }

    private void mapFoodInfant(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.addAll(List.of(PRODUCT_TYPE, PRODUCT_SUB_TYPE, PRODUCT_FORM, VARIANT_NAME,
                AGE_GROUPS, NET_QUANTITY, NET_QUANTITY_UNIT, MANUFACTURER_NAME));

        ProductAttributeFoodInfantDto dto = new ProductAttributeFoodInfantDto();
        Long typeId = resolve(masters, MasterIndex.PRODUCT_TYPE, mapped.getProductCategoryId(),
                row, PRODUCT_TYPE, mapped);
        dto.setProductTypeId(typeId);
        dto.setProductSubTypeId(resolve(masters, MasterIndex.PRODUCT_SUB_TYPE, typeId,
                row, PRODUCT_SUB_TYPE, mapped));
        dto.setProductFormId(resolve(masters, MasterIndex.PRODUCT_FORM, mapped.getProductCategoryId(),
                row, PRODUCT_FORM, mapped));
        dto.setVariantName(CellCoercion.text(row, VARIANT_NAME));
        dto.setAgeGroupIds(resolveAll(masters, MasterIndex.AGE_GROUP, row, AGE_GROUPS, mapped));
        dto.setNetQuantity(CellCoercion.decimal(row, NET_QUANTITY));
        dto.setNetQuantityUnitId(resolve(masters, MasterIndex.NET_QUANTITY_UNIT,
                mapped.getProductCategoryId(), row, NET_QUANTITY_UNIT, mapped));
        dto.setManufacturerName(CellCoercion.text(row, MANUFACTURER_NAME));
        mapped.setFoodInfant(dto);
    }

    private void mapConsumable(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.addAll(List.of(DEVICE_CATEGORY, DEVICE_SUB_CATEGORY, MATERIAL_TYPES, DIMENSION_SIZE,
                SPECIFICATION_UNIT, STERILE, DISPOSABLE, PURPOSE, MANUFACTURER_NAME,
                MANUFACTURER_LICENSE_NUMBER, ISO_CERTIFIED));

        ProductAttributeConsumableMedicalDto dto = new ProductAttributeConsumableMedicalDto();
        Long deviceCategoryId = resolve(masters, MasterIndex.DEVICE_CATEGORY,
                mapped.getProductCategoryId(), row, DEVICE_CATEGORY, mapped);
        dto.setDeviceCategoryId(deviceCategoryId);
        Long deviceSubCategoryId = resolve(masters, MasterIndex.DEVICE_SUB_CATEGORY,
                deviceCategoryId, row, DEVICE_SUB_CATEGORY, mapped);
        dto.setDeviceSubCategoryId(deviceSubCategoryId);
        dto.setMaterialTypeIds(resolveAll(masters, MasterIndex.MATERIAL_TYPE, row, MATERIAL_TYPES, mapped));
        dto.setDimensionSize(CellCoercion.text(row, DIMENSION_SIZE));
        dto.setDeviceSpecificationUnitId(resolve(masters, MasterIndex.DEVICE_SPECIFICATION_UNIT,
                deviceSubCategoryId, row, SPECIFICATION_UNIT, mapped));
        // Stored as free text on the entity, so the sheet's own wording is kept.
        dto.setSterileOrNonSterile(CellCoercion.text(row, STERILE));
        dto.setDisposalOrNonDisposal(CellCoercion.text(row, DISPOSABLE));
        dto.setPurpose(CellCoercion.text(row, PURPOSE));
        dto.setManufacturerName(CellCoercion.text(row, MANUFACTURER_NAME));
        dto.setManufacturerLicenseNumber(CellCoercion.code(row, MANUFACTURER_LICENSE_NUMBER));
        dto.setIsISOCertified(CellCoercion.flag(row, ISO_CERTIFIED));
        mapped.setConsumable(dto);
    }

    private void mapNonConsumable(CsvFile.Row row, MasterIndex masters, MappedRow mapped, Set<String> used) {
        used.addAll(List.of(DEVICE_CATEGORY, DEVICE_SUB_CATEGORY, MODEL_NAME, DEVICE_CLASSIFICATION,
                PURPOSE, DIMENSION_SIZE, SPECIFICATION_UNIT, MATERIAL_TYPES, POWER_SOURCE,
                WARRANTY_PERIOD_MONTHS, SERVICE_AVAILABILITY, MANUFACTURER_NAME, COUNTRY_OF_ORIGIN));

        ProductAttributeNonConsumableMedicalDto dto = new ProductAttributeNonConsumableMedicalDto();
        Long deviceCategoryId = resolve(masters, MasterIndex.DEVICE_CATEGORY,
                mapped.getProductCategoryId(), row, DEVICE_CATEGORY, mapped);
        dto.setDeviceCategoryId(deviceCategoryId);
        Long deviceSubCategoryId = resolve(masters, MasterIndex.DEVICE_SUB_CATEGORY,
                deviceCategoryId, row, DEVICE_SUB_CATEGORY, mapped);
        dto.setDeviceSubCategoryId(deviceSubCategoryId);
        dto.setModelName(CellCoercion.text(row, MODEL_NAME));
        dto.setDeviceClassification(CellCoercion.text(row, DEVICE_CLASSIFICATION));
        dto.setPurpose(CellCoercion.text(row, PURPOSE));
        dto.setDimensionSize(CellCoercion.text(row, DIMENSION_SIZE));
        dto.setDeviceSpecificationUnitId(resolve(masters, MasterIndex.DEVICE_SPECIFICATION_UNIT,
                deviceSubCategoryId, row, SPECIFICATION_UNIT, mapped));
        dto.setMaterialTypeIds(resolveAll(masters, MasterIndex.MATERIAL_TYPE, row, MATERIAL_TYPES, mapped));
        dto.setPowerSourceId(resolve(masters, MasterIndex.POWER_SOURCE, null, row, POWER_SOURCE, mapped));
        dto.setWarrantyPeriod(CellCoercion.code(row, WARRANTY_PERIOD_MONTHS));
        dto.setServiceAvailability(CellCoercion.flag(row, SERVICE_AVAILABILITY));
        dto.setManufacturerName(CellCoercion.text(row, MANUFACTURER_NAME));
        dto.setCountryId(resolve(masters, MasterIndex.COUNTRY, null, row, COUNTRY_OF_ORIGIN, mapped));
        mapped.setNonConsumable(dto);
    }

    // ===== master resolution =====

    private Long resolve(MasterIndex masters, String index, Long parentId,
                         CsvFile.Row row, String column, MappedRow mapped) {
        String name = CellCoercion.text(row, column);
        if (name == null) {
            return null;
        }
        Long id = masters.findScoped(index, parentId, name);
        if (id == null) {
            mapped.addWarning("Column '" + column + "': \"" + name + "\" is not in the master list — left blank");
        }
        return id;
    }

    private List<Long> resolveAll(MasterIndex masters, String index,
                                  CsvFile.Row row, String column, MappedRow mapped) {
        List<String> names = CellCoercion.multi(row, column);
        if (names.isEmpty()) {
            return List.of();
        }
        List<String> unresolved = new ArrayList<>();
        List<Long> ids = masters.findAll(index, names, unresolved);
        for (String name : unresolved) {
            mapped.addWarning("Column '" + column + "': \"" + name + "\" is not in the master list — skipped");
        }
        return ids;
    }

    /**
     * Flags values the sheet supplied that this product's category has no column for,
     * so an import never drops data silently.
     */
    private void warnAboutUnstoredColumns(CsvFile.Row row, MappedRow mapped, Set<String> used) {
        for (String column : ProductBulkColumns.TEMPLATE) {
            if (!used.contains(column) && row.isPresent(column)) {
                mapped.addWarning("Column '" + column + "' is not stored for category \""
                        + mapped.getCategoryName() + "\" — value ignored");
            }
        }
    }

    /**
     * A line that cannot be written. Reported against its line; the rest of the file continues.
     */
    public static class RowRejectedException extends RuntimeException {
        public RowRejectedException(String message) {
            super(message);
        }
    }
}
