package tiameds.pharmabackend.enums;

/**
 * Lifecycle of a warehouse distribution / stock transfer. Trimmed to the states the
 * system actually stops in: created, dispatched (source stock out), received
 * (destination stock in). STOCK_REJECTED is the terminal branch when the destination
 * refuses the goods.
 */
public enum DistributionStatus {
    DISTRIBUTION_CREATED,
    PRODUCTS_DISPATCHED,
    STOCK_RECEIVED,
    STOCK_REJECTED
}
