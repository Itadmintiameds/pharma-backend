package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAdjustmentDto {

    private Long stockAdjustmentId;
    private String pharmacyId;
    private String warehouseId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private String stockAdjustmentNo;
    private LocalDateTime stockAdjustmentDate;
    private Long actualStockQty;
    private Long adjustmentQty;
    private String adjustmentType;
    private Long adjustmentValue;
    private String stockAdjustmentReason;
    private String stockAdjustmentRemarks;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
