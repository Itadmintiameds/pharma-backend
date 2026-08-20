package tiameds.pharmabackend.service.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionDispatchRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionReceiveRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionResponse;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionSummaryResponse;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

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

    /**
     * OUT leg: deduct dispatched quantities from the source, record PRODUCTS_DISPATCHED.
     * The request carries the quantities actually shipped (which may be less than the
     * issued quantity) and an optional per-line remark; any line omitted from it
     * defaults to its issued quantity. Pass {@code null} to dispatch every line in full.
     */
    void dispatch(Long distributionId, WarehouseDistributionDispatchRequest request, UserDetails user);

    /**
     * IN leg: add received quantities to the destination, record STOCK_RECEIVED.
     * The request carries the products/quantities that actually arrived; any
     * dispatched line omitted from it defaults to its dispatched quantity. Pass
     * {@code null} to receive every line at its dispatched quantity.
     */
    void receive(Long distributionId, WarehouseDistributionReceiveRequest request, UserDetails user);

    /** Read one distribution with its lines and current status. */
    WarehouseDistributionResponse getById(Long distributionId);

    /**
     * List the distributions the acting user's warehouse is involved in — outgoing
     * (allocationRequestedBy) plus incoming (it is the destination) — as compact
     * summary rows (both ends resolved to their store name, product/quantity totals
     * and latest status), newest first.
     */
    List<WarehouseDistributionSummaryResponse> getAll(UserDetails user);

    /** Summary rows for distributions shipped FROM the acting user's warehouse (source). */
    List<WarehouseDistributionSummaryResponse> getBySource(UserDetails user);

    /** Summary rows for distributions shipped TO the acting user's warehouse (destination). */
    List<WarehouseDistributionSummaryResponse> getByDestination(UserDetails user);

    /**
     * Next allocation number for the UI to display on a blank create form.
     * Preview only — the authoritative number is assigned at create time.
     */
    String peekNextAllocationNo();
}
