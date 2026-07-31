package tiameds.pharmabackend.dto.product;

import lombok.Data;

/**
 * Dashboard KPI cards for product expiry, scoped to the current pharmacy.
 * The four expiry buckets classify each product once by the nearest (earliest)
 * expiry date among its in-stock batches (stock > 0 in pharma_inventory).
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
