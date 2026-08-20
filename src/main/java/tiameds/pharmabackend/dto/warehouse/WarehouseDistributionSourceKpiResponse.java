package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// KPI card figures for the distribution source screen, scoped to the acting location
// (the warehouse/pharmacy shipping stock). All three count distributions by their
// CURRENT status with that location as the SOURCE of the distribution.
@Data
public class WarehouseDistributionSourceKpiResponse {

    // Allocations created by/from this location but not yet dispatched —
    // current status DISTRIBUTION_CREATED.
    private Long readyToDispatch;

    // Dispatched from this location, awaiting receipt at the destination —
    // current status PRODUCTS_DISPATCHED.
    private Long pendingReceipt;

    // Fully completed transfers — the destination has received the stock
    // (current status STOCK_RECEIVED).
    private Long completed;
}
