package tiameds.pharmabackend.service.product.bulk;

import java.util.List;

/**
 * Column headers of the product bulk-upload template, in template order.
 * <p>
 * Lookups are case-insensitive (see {@link CsvFile.Row#get}), so an uploaded file
 * only has to match the header text, not its casing or column order.
 */
public final class ProductBulkColumns {

    private ProductBulkColumns() {
    }

    // --- identity / grouping ---
    public static final String PRODUCT_CODE = "Product Code";
    public static final String PRODUCT_NAME = "Product Name";
    public static final String BRAND_NAME = "Brand Name";
    public static final String MANUFACTURER_NAME = "Manufacturer Name";
    public static final String CATEGORY = "Category";
    public static final String HSN_CODE = "HSN Code";
    public static final String GST_PERCENT = "GST %";

    // --- packaging ---
    public static final String PURCHASE_UNIT = "Purchase Unit";
    public static final String SMALLEST_UNIT = "Smallest Unit";
    public static final String UNITS_PER_PURCHASE_UNIT = "Units Per Purchase Unit";

    // --- batch ---
    public static final String BATCH_NUMBER = "Batch Number";
    public static final String MANUFACTURING_DATE = "Manufacturing Date";
    public static final String EXPIRY_DATE = "Expiry Date";
    public static final String PURCHASE_PRICE_PER_UNIT = "Purchase Price Per Unit";
    public static final String MRP_PER_PACK = "MRP Per Pack";
    public static final String SELLING_PRICE_PER_UNIT = "Selling Price Per Unit";
    public static final String STOCK_IN_SMALLEST_UNITS = "Stock In Smallest Units";
    public static final String RACK_LOCATION = "Rack Location";

    // --- category attributes ---
    public static final String DOSAGE_FORM = "Dosage Form";
    public static final String PRODUCT_TYPE = "Product Type";
    public static final String PRODUCT_SUB_TYPE = "Product Sub Type";
    public static final String PRODUCT_FORM = "Product Form";
    public static final String VARIANT_NAME = "Variant Name";
    public static final String NET_QUANTITY = "Net Quantity";
    public static final String NET_QUANTITY_UNIT = "Net Quantity Unit";
    public static final String AGE_GROUPS = "Age Groups";
    public static final String GENDER = "Gender";
    public static final String MANUFACTURER_LICENSE_NUMBER = "Manufacturer License Number";
    public static final String DRUG_SCHEDULE = "Drug Schedule";
    public static final String MOLECULES = "Molecules";
    public static final String THERAPEUTIC_CATEGORY = "Therapeutic Category";
    public static final String THERAPEUTIC_SUBCATEGORY = "Therapeutic Subcategory";
    public static final String STRENGTH_COMPOSITION = "Strength Composition";
    public static final String FLAVOUR = "Flavour";
    public static final String FSSAI_LICENSE_NUMBER = "FSSAI License Number";
    public static final String INTENDED_USE_AREAS = "Intended Use Areas";
    public static final String SKIN_TYPES = "Skin Types";
    public static final String HAIR_TYPES = "Hair Types";
    public static final String FRAGRANCE = "Fragrance";
    public static final String DEVICE_CATEGORY = "Device Category";
    public static final String DEVICE_SUB_CATEGORY = "Device Sub Category";
    public static final String MATERIAL_TYPES = "Material Types";
    public static final String DIMENSION_SIZE = "Dimension Size";
    public static final String SPECIFICATION_UNIT = "Specification Unit";
    public static final String PURPOSE = "Purpose";
    public static final String STERILE = "Sterile";
    public static final String DISPOSABLE = "Disposable";
    public static final String ISO_CERTIFIED = "ISO Certified";
    public static final String MODEL_NAME = "Model Name";
    public static final String DEVICE_CLASSIFICATION = "Device Classification";
    public static final String POWER_SOURCE = "Power Source";
    public static final String WARRANTY_PERIOD_MONTHS = "Warranty Period Months";
    public static final String SERVICE_AVAILABILITY = "Service Availability";
    public static final String COUNTRY_OF_ORIGIN = "Country Of Origin";

    /**
     * The full template header row, in order — also served by the template download endpoint.
     */
    public static final List<String> TEMPLATE = List.of(
            PRODUCT_CODE, PRODUCT_NAME, BRAND_NAME, MANUFACTURER_NAME, CATEGORY, HSN_CODE, GST_PERCENT,
            PURCHASE_UNIT, SMALLEST_UNIT, UNITS_PER_PURCHASE_UNIT,
            BATCH_NUMBER, MANUFACTURING_DATE, EXPIRY_DATE, PURCHASE_PRICE_PER_UNIT, MRP_PER_PACK,
            SELLING_PRICE_PER_UNIT, STOCK_IN_SMALLEST_UNITS, RACK_LOCATION,
            DOSAGE_FORM, PRODUCT_TYPE, PRODUCT_SUB_TYPE, PRODUCT_FORM, VARIANT_NAME, NET_QUANTITY,
            NET_QUANTITY_UNIT, AGE_GROUPS, GENDER, MANUFACTURER_LICENSE_NUMBER, DRUG_SCHEDULE, MOLECULES,
            THERAPEUTIC_CATEGORY, THERAPEUTIC_SUBCATEGORY, STRENGTH_COMPOSITION, FLAVOUR,
            FSSAI_LICENSE_NUMBER, INTENDED_USE_AREAS, SKIN_TYPES, HAIR_TYPES, FRAGRANCE,
            DEVICE_CATEGORY, DEVICE_SUB_CATEGORY, MATERIAL_TYPES, DIMENSION_SIZE, SPECIFICATION_UNIT,
            PURPOSE, STERILE, DISPOSABLE, ISO_CERTIFIED, MODEL_NAME, DEVICE_CLASSIFICATION,
            POWER_SOURCE, WARRANTY_PERIOD_MONTHS, SERVICE_AVAILABILITY, COUNTRY_OF_ORIGIN);

    /**
     * Columns that must be present for the file to be processable at all.
     */
    public static final List<String> REQUIRED_HEADERS = List.of(
            PRODUCT_NAME, CATEGORY, PURCHASE_UNIT, SMALLEST_UNIT, UNITS_PER_PURCHASE_UNIT, BATCH_NUMBER);
}
