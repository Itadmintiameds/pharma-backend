package tiameds.pharmabackend.dto.purchase;

import lombok.Data;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;

import java.time.LocalDateTime;

@Data
public class InventoryAuditDto {

    private Long inventoryAuditId;
    private Long inventoryId;
    private Long purchaseDetailsId;
    private Long billingId;
    private StockMovement stockMovement;
    private TransactionType transactionType;
    private Long changeStock;
    private Long remainingStock;
    private String changedBy;
    private LocalDateTime changedAt;
}
