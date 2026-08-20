package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// KPI card figures for the "my requests" screen, scoped to the acting warehouse as the
// REQUESTING warehouse (allocationRequestedBy). totalTransfers is an all-time count;
// the other three count distributions by their CURRENT status.
@Data
public class WarehouseDistributionRequestedByKpiResponse {

    // All distributions ever requested by this warehouse, any status.
    private Long totalTransfers;

    // Fully completed transfers — current status STOCK_RECEIVED.
    private Long completed;

    // Dispatched, awaiting receipt — current status PRODUCTS_DISPATCHED.
    private Long pending;

    // Created but not yet dispatched — current status DISTRIBUTION_CREATED.
    private Long readyToDispatch;
}
