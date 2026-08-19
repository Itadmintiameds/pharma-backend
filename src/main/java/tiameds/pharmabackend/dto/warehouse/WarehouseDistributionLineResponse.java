package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

@Data
public class WarehouseDistributionLineResponse {

    private Long warehouseDistributionDetailsId;
    private String productId;
    private String packagingId;
    private String batchId;
    private Long issueQuantity;
    private Long receivedQuantity;
    private Long damagedQuantity;
    private String remarks;
}
