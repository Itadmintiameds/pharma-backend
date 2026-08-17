package tiameds.pharmabackend.service.warehouse.stock;

import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionDetails;
import tiameds.pharmabackend.enums.TransactionType;

import java.time.LocalDateTime;

/**
 * One leg (IN or OUT) of a stock movement — either a stock transfer (distribution)
 * or a supplier purchase. product/packaging/batch are already managed entities.
 *
 * <p>Exactly one of {@code distributionDetails} / {@code purchaseDetails} is set,
 * matching {@code transactionType}; the audit row links back to whichever is present.
 *
 * @param locationId          warehouse_id or pharmacy_id the stock moves from/into
 * @param product             product being moved
 * @param packaging           packaging level being moved
 * @param batch               batch being moved (batch identity is preserved across the movement)
 * @param quantity            units to add or remove (always positive)
 * @param transactionType     STOCK_TRANSFER or PURCHASE
 * @param distributionDetails allocation line this movement belongs to (transfers); recorded on the audit row
 * @param purchaseDetails     purchase line this movement belongs to (purchases); recorded on the audit row
 * @param actor               user performing the movement (created_by / changed_by)
 * @param timestamp           when the movement happened
 */
public record StockAdjustment(
        String locationId,
        ProductDetails product,
        PackagingDetails packaging,
        BatchDetails batch,
        long quantity,
        TransactionType transactionType,
        WarehouseDistributionDetails distributionDetails,
        PurchaseDetails purchaseDetails,
        String actor,
        LocalDateTime timestamp
) {

    // Back-compat for stock transfers: existing distribution call sites pass a
    // distribution line and no purchase reference.
    public StockAdjustment(
            String locationId,
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch,
            long quantity,
            WarehouseDistributionDetails distributionDetails,
            String actor,
            LocalDateTime timestamp) {
        this(locationId, product, packaging, batch, quantity,
                TransactionType.STOCK_TRANSFER, distributionDetails, null, actor, timestamp);
    }
}
