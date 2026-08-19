package tiameds.pharmabackend.service.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionReceiveRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionResponse;
import tiameds.pharmabackend.entity.UserDetails;

/**
 * Orchestrates a stock transfer across its lifecycle. Each state transition is a
 * separate call so it can be triggered independently by the UI:
 * <pre>
 *   createAllocation  -> DISTRIBUTION_CREATED
 *   dispatch          -> PRODUCTS_DISPATCHED   (source stock leaves)
 *   receive           -> STOCK_RECEIVED        (destination stock arrives)
 * </pre>
 * Works for warehouse->pharmacy and pharmacy->pharmacy without branching, because
 * the inventory legs are resolved from the distribution's source/destination
 * {@code LocationType} via {@code InventoryAdjusters}.
 */
public interface WarehouseDistributionService {

    /** Persist the allocation header + lines and record DISTRIBUTION_CREATED. */
    WarehouseDistributionResponse createAllocation(WarehouseDistributionRequest request, UserDetails user);

    /** OUT leg: deduct issued quantities from the source, record PRODUCTS_DISPATCHED. */
    void dispatch(Long distributionId, UserDetails user);

    /**
     * IN leg: add received quantities to the destination, record STOCK_RECEIVED.
     * The request carries the products/quantities that actually arrived; any
     * dispatched line omitted from it defaults to its issued quantity. Pass
     * {@code null} to receive every line at its issued quantity.
     */
    void receive(Long distributionId, WarehouseDistributionReceiveRequest request, UserDetails user);

    /** Read one distribution with its lines and current status. */
    WarehouseDistributionResponse getById(Long distributionId);

    /**
     * Next allocation number for the UI to display on a blank create form.
     * Preview only — the authoritative number is assigned at create time.
     */
    String peekNextAllocationNo();
}
