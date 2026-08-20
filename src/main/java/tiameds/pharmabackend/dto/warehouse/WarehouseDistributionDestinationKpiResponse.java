package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

// KPI card figures for the distribution destination screen, scoped to the acting
// location (the warehouse/pharmacy receiving stock). All three are computed against
// that location as the DESTINATION of a distribution.
@Data
public class WarehouseDistributionDestinationKpiResponse {

    // Distributions dispatched TO this location that are still awaiting receipt —
    // current status PRODUCTS_DISPATCHED (dispatched but not yet received/rejected).
    private Long pendingReceipts;

    // Number of distributions received (STOCK_RECEIVED) by this location today.
    private Long receivedToday;

    // Total quantity of products received by this location today (sum of received
    // quantity across the lines of today's receipts).
    private Long productsReceivedToday;
}
