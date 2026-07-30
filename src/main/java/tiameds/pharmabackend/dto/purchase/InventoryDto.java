package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryDto {

    private Long inventoryId;
    private String productId;
    private String packagingId;
    private String batchId;
    private Long totalStock;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
