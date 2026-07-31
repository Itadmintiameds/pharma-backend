package tiameds.pharmabackend.dto.product;

import java.util.List;

import lombok.Data;

/**
 * Request to add a new package (packaging) to an existing product, optionally
 * with a set of batches created under that new package. IDs are generated
 * server-side, so any packagingId/batchId sent in the body is ignored.
 */
@Data
public class AddPackageRequest {
    private String purchaseUnit;
    private Long purchaseUnitContains;
    private String smallestUnit;

    // optional batches to create under the new package
    private List<BatchDetailsDto> batches;
}
