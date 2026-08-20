package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.DistributionStatus;
import tiameds.pharmabackend.enums.LocationType;

import java.time.LocalDateTime;

// Compact row for the distribution list screen: identifiers, both ends (resolved
// to their store name), line totals and the latest status — no per-line detail.
@Data
public class WarehouseDistributionSummaryResponse {

    private Long warehouseDistributionId;
    private String allocationNo;

    // Relative to the acting warehouse: OUTGOING (it requested/ships) or INCOMING
    // (it is the destination). Lets a merged list distinguish the two.
    private String direction;

    // From store (source): type + id + resolved name
    private LocationType fromType;
    private String fromId;
    private String fromStore;

    // To store (destination): type + id + resolved name
    private LocationType toType;
    private String toId;
    private String toStore;

    private Long productsCount;     // number of distinct products in the allocation
    private Long totalQuantity;     // sum of issued quantity across all lines

    private DistributionStatus currentStatus;
    private LocalDateTime allocationDate;
}
