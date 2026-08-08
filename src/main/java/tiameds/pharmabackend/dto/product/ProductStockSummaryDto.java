package tiameds.pharmabackend.dto.product;

import java.time.LocalDate;

import lombok.Data;

/**
 * Row for the "all products of a pharmacy" listing.
 * Stock is aggregated from pharma_inventory (total_stock); batch status is
 * bucketed over the in-stock inventory rows using the batch expiryDate.
 */
@Data
public class ProductStockSummaryDto {
    private String productId;
    private String productName;
    private String brandName;

    // category master
    private Long productCategoryId;
    private String productCategoryName;

    // manufacturer, resolved from the product's category-specific attribute row
    private String manufacturerName;

    private Long totalStock;

    // counts of in-stock batches bucketed by expiry
    private long activeBatches;
    private long nearExpiryBatches;
    private long expiredBatches;

    // worst-case status across in-stock batches:
    // EXPIRED > NEAR_EXPIRY > ACTIVE > OUT_OF_STOCK
    private String overallStatus;

    // earliest expiryDate among in-stock batches (most urgent), null if none
    private LocalDate nearestExpiryDate;
}
