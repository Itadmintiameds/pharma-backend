package tiameds.pharmabackend.service.warehouse.stock;

import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.enums.LocationType;

import java.util.Optional;

/**
 * Applies one leg of a stock transfer against a specific inventory backing
 * (warehouse inventory vs pharmacy inventory). One implementation per
 * {@link LocationType}; the correct one is resolved by {@code InventoryAdjusters}
 * from the distribution's source/destination type.
 * <p>
 * Call from within a {@code @Transactional} boundary so the stock change and its
 * audit row commit together.
 */
public interface InventoryAdjuster {

    /** The location type this adjuster handles. */
    LocationType locationType();

    /**
     * OUT leg (source): validates sufficient stock, lowers it, writes an OUT audit row.
     *
     * @throws IllegalStateException if there is no stock row for the batch, or it would go negative
     */
    void decrement(StockAdjustment adj);

    /**
     * IN leg (destination): finds or creates the stock row, raises it, writes an IN audit row.
     */
    void increment(StockAdjustment adj);

    /**
     * Stock this location holds for the batch, in smallest units — or empty when the
     * location has no stock row for it at all (as opposed to a row that has run down to 0).
     */
    Optional<Long> currentStock(String locationId, ProductDetails product,
                                PackagingDetails packaging, BatchDetails batch);
}
