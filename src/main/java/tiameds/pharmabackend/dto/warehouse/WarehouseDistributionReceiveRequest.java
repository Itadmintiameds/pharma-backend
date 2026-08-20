package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.util.List;

// Payload for the receive step: the products/quantities that actually arrived at
// the destination. Lines are optional — any dispatched line omitted here defaults
// to its dispatched quantity.
@Data
public class WarehouseDistributionReceiveRequest {

    private List<WarehouseDistributionReceiveLineRequest> lines;
}