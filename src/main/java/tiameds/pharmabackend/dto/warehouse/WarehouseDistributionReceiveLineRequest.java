package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// One line being received: identifies the dispatched line and the quantity that
// actually arrived (may be less than the dispatched quantity for partial receipts).
@Data
public class WarehouseDistributionReceiveLineRequest {

    private Long warehouseDistributionDetailsId;
    private Long receivedQuantity;
    private Long damagedQuantity;   // damaged / not-received units for this line (optional, defaults to 0)
    private String remarks;         // receiver's note for this line (optional)
}