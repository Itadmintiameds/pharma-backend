package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// One allocation line to be issued.
@Data
public class WarehouseDistributionLineRequest {

    private String productId;
    private String packagingId;
    private String batchId;
    private Long issueQuantity;
}
