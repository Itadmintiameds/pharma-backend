package tiameds.pharmabackend.dto.product;

import java.util.List;

import lombok.Data;

/**
 * A single package (packaging) of a product together with the batches
 * that belong to it.
 */
@Data
public class PackageWithBatchesDto {
    private String packagingId;
    private String purchaseUnit;
    private Long purchaseUnitContains;
    // private String smallestUnit;
    private Long purchaseSmallestUnitId;
    private String purchaseSmallestUnitName;

    private List<BatchDetailsDto> batches;
}
