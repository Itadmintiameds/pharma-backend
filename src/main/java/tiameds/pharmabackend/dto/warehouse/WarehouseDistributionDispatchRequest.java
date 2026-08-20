package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.util.List;

// Payload for the dispatch step: the products/quantities actually shipped from the
// source. Lines are optional — any allocation line omitted here defaults to its
// issued quantity (full dispatch).
@Data
public class WarehouseDistributionDispatchRequest {

    private List<WarehouseDistributionDispatchLineRequest> lines;
}
