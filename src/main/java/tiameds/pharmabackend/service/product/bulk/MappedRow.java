package tiameds.pharmabackend.service.product.bulk;

import lombok.Data;
import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeConsumableMedicalDto;
import tiameds.pharmabackend.dto.product.ProductAttributeCosmeticsDto;
import tiameds.pharmabackend.dto.product.ProductAttributeDrugDto;
import tiameds.pharmabackend.dto.product.ProductAttributeFoodInfantDto;
import tiameds.pharmabackend.dto.product.ProductAttributeNonConsumableMedicalDto;
import tiameds.pharmabackend.dto.product.ProductAttributeSupplementsDto;

import java.util.ArrayList;
import java.util.List;

/**
 * One validated CSV line, expressed in the DTOs the product service already speaks.
 * <p>
 * Product-level fields repeat on every line of a product's group; the writer takes
 * them from the group's first line and uses the rest only for packaging and batches.
 */
@Data
public class MappedRow {

    private int line;
    private String productCode;

    // --- product ---
    private String productName;
    private String brandName;
    private String hsnNo;
    private String gstPercentage;
    private Long productCategoryId;
    private String categoryName;
    private CategoryKind categoryKind;

    // --- packaging ---
    private Long purchaseSmallestUnitId;
    private String purchaseUnitName;
    private String smallestUnitName;
    private Long purchaseUnitContains;

    // --- batch ---
    private BatchDetailsDto batch;

    /**
     * Opening stock for this batch, in smallest units. Null or 0 writes no inventory row.
     */
    private Long stockInSmallestUnits;

    // --- exactly one of these is set, per the product's category ---
    private ProductAttributeDrugDto drug;
    private ProductAttributeCosmeticsDto cosmetics;
    private ProductAttributeSupplementsDto supplements;
    private ProductAttributeFoodInfantDto foodInfant;
    private ProductAttributeConsumableMedicalDto consumable;
    private ProductAttributeNonConsumableMedicalDto nonConsumable;

    private List<String> warnings = new ArrayList<>();

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Groups a product's lines: two lines belong to the same package when they
     * describe the same unit pair and the same pack size.
     */
    public String packagingKey() {
        return purchaseSmallestUnitId + "/" + purchaseUnitContains;
    }

    /**
     * Which attribute table a product category writes into.
     */
    public enum CategoryKind {
        DRUG,
        COSMETIC,
        SUPPLEMENT,
        FOOD_INFANT,
        CONSUMABLE_DEVICE,
        NON_CONSUMABLE_DEVICE,
        /**
         * A category with no attribute table of its own — the product and its batches
         * are still created, the category-specific columns are reported as unstored.
         */
        NONE
    }
}
