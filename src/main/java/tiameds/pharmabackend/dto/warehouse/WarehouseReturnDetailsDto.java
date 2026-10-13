package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseReturnDetailsDto {

    private Long warehouseReturnDetailId;
    // Damaged distribution line being returned; send it when returning from the damaged-not-returned list.
    private Long warehouseDistributionDetailsId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private String purchaseUnit;
    private Long purchaseUnitContains;
    private String smallestUnit;
    private Long returnQuantity;
    private Long dispatchQuantity;
    private Long receivedQuantity;
    private Long notReceivedQuantity;
    private String returnReason;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
