package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseReturnDetailsDto {

    private Long warehouseReturnDetailId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
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
