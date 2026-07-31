package tiameds.pharmabackend.dto.product;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * Complete details of a single product with its packages, where the batches
 * are grouped under the package they belong to. Batches that are not linked
 * to any package are returned in {@code unassignedBatches}.
 */
@Data
public class ProductDetailResponseDto {
    private String productId;
    private String pharmacyId;
    private Long productCategoryId;
    private String productName;
    private String brandName;
    private BigDecimal gstPercentage;
    private String hsnNo;

    private List<PackageWithBatchesDto> packages;
    private List<BatchDetailsDto> unassignedBatches;
}
