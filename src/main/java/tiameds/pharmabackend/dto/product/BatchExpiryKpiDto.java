package tiameds.pharmabackend.dto.product;

import lombok.Data;

/**
 * Dashboard KPI cards for batch expiry, scoped to the current location
 * (pharmacy or warehouse). Unlike {@link ProductExpiryKpiDto}, each in-stock
 * batch (total_stock > 0) is counted independently into exactly one expiry
 * bucket by its own expiry date — no priority collapsing is applied, so the
 * four buckets sum to the number of in-stock batches.
 * A batch with no expiry date is treated as healthy.
 * totalBatches counts ALL batches of the location (including zero-stock ones),
 * mirroring how totalProducts counts all products; the gap between totalBatches
 * and the sum of the four buckets is the out-of-stock batches.
 */
@Data
public class BatchExpiryKpiDto {
    // Expired (Cannot Sell) - expiry is before today
    private long expiredBatches;

    // Expiring in 0-30 Days
    private long expiring0To30DaysBatches;

    // Expiring in 31-60 Days
    private long expiring31To60DaysBatches;

    // Healthy (> 60 Days, or no expiry date)
    private long healthyAbove60DaysBatches;

    // Total batches of the location (all batches, including out-of-stock)
    private long totalBatches;

    // Total products of the location (all products) - same as ProductExpiryKpiDto
    private long totalProducts;
}
