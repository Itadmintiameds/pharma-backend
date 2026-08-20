package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// One line being dispatched: identifies the allocation line and the quantity actually
// shipped from the source (may be less than the issued quantity when shipping short).
@Data
public class WarehouseDistributionDispatchLineRequest {

    private Long warehouseDistributionDetailsId;
    private Long dispatchedQuantity;
    private String remarks;          // sender's note for this line (optional, e.g. reason for shipping short)
}
