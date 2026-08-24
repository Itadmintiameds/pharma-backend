package tiameds.pharmabackend.dto.product;

import lombok.Data;

/**
 * Dashboard KPI cards for product expiry, scoped to the current pharmacy.
 * The four expiry buckets classify each product once by priority across its
 * in-stock batches (stock > 0 in pharma_inventory):
 * NEAR_EXPIRY (0-60 days) > HEALTHY (> 60 days) > EXPIRED. A near-expiry batch
 * therefore takes precedence over an expired one; within near-expiry the nearest
 * date decides the 0-30 vs 31-60 bucket.
 * Products with no in-stock batches are excluded from the buckets but still
 * counted in totalProducts.
 */
@Data
public class ProductExpiryKpiDto {
    // Expired (Cannot Sell) - nearest in-stock expiry is before today
    private long expired;

    // Expiring in 0-30 Days
    private long expiring0To30Days;

    // Expiring in 31-60 Days
    private long expiring31To60Days;

    // Healthy (> 60 Days)
    private long healthyAbove60Days;

    // Total Products across all variants (all products of the pharmacy)
    private long totalProducts;
}
