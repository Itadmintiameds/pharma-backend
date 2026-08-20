package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.warehouse.*;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistribution;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionStatus;
import tiameds.pharmabackend.enums.DistributionStatus;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.exception.ResourceNotFoundException;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.PackagingDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseDistributionDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseDistributionRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseDistributionStatusRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.PharmacyOrganizationService;
import tiameds.pharmabackend.service.impl.warehouse.stock.InventoryAdjusters;
import tiameds.pharmabackend.service.warehouse.WarehouseDistributionService;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseDistributionServiceImpl implements WarehouseDistributionService {

    private final WarehouseDistributionRepository distributionRepository;
    private final WarehouseDistributionDetailsRepository detailsRepository;
    private final WarehouseDistributionStatusRepository statusRepository;
    private final ProductDetailsRepository productRepository;
    private final PackagingDetailsRepository packagingRepository;
    private final BatchDetailsRepository batchRepository;
    private final AllocationNoGenerator allocationNoGenerator;
    private final InventoryAdjusters adjusters;
    private final WarehouseRepository warehouseRepository;
    private final PharmacyDetailsRepository pharmacyRepository;
    private final PharmacyOrganizationService organizationService;
    private final LocationContextResolver locationContextResolver;

    @Override
    public WarehouseDistributionResponse createAllocation(WarehouseDistributionRequest request, UserDetails user) {
        String actor = actorOf(user);
        LocalDateTime now = LocalDateTime.now();

        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one allocation line is required");
        }

        validateAllocation(request, user);

        WarehouseDistribution dist = new WarehouseDistribution();
        dist.setAllocationMode(request.getAllocationMode());
        // Server-assigned: any client-supplied allocationNo is ignored so the number
        // is authoritative and unique.
        dist.setAllocationNo(allocationNoGenerator.generate());
        dist.setAllocationDate(now);
        dist.setDistributionType(request.getDistributionType());
        dist.setReference(request.getReference());
        dist.setRemarks(request.getRemarks());
        dist.setSourceType(request.getSourceType());
        dist.setSourceId(request.getSourceId());
        dist.setDestinationType(request.getDestinationType());
        dist.setDestinationId(request.getDestinationId());
        // The requesting warehouse is the one the acting warehouse manager is
        // operating as for this request (X-Warehouse-Id, or their only warehouse),
        // not a client input — null for non-warehouse-manager actors.
        dist.setAllocationRequestedBy(
                locationContextResolver.isWarehouseManager(user)
                        ? locationContextResolver.resolve(user).getLocationId()
                        : null);
        dist.setCreatedBy(actor);
        dist.setCreatedAt(now);
        WarehouseDistribution saved = distributionRepository.save(dist);

        for (WarehouseDistributionLineRequest lr : request.getLines()) {
            WarehouseDistributionDetails line = new WarehouseDistributionDetails();
            line.setWarehouseDistribution(saved);
            line.setProduct(productRepository.getReferenceById(lr.getProductId()));
            line.setPackaging(packagingRepository.getReferenceById(lr.getPackagingId()));
            line.setBatch(batchRepository.getReferenceById(lr.getBatchId()));
            line.setIssueQuantity(lr.getIssueQuantity());
            line.setCreatedBy(actor);
            line.setCreatedAt(now);
            detailsRepository.save(line);
        }

        appendStatus(saved, DistributionStatus.DISTRIBUTION_CREATED, actor, now);
        log.info("Created distribution {} ({} {} -> {} {}) with {} line(s)",
                saved.getWarehouseDistributionId(), saved.getSourceType(), saved.getSourceId(),
                saved.getDestinationType(), saved.getDestinationId(), request.getLines().size());
        return getById(saved.getWarehouseDistributionId());
    }

    @Override
    public void dispatch(Long distributionId, UserDetails user) {
        String actor = actorOf(user);
        LocalDateTime now = LocalDateTime.now();
        WarehouseDistribution dist = getOrThrow(distributionId);

        // Guard: only a freshly created allocation may be dispatched — prevents
        // dispatching twice (which would deduct source stock twice).
        requireStatus(distributionId, DistributionStatus.DISTRIBUTION_CREATED, "dispatch");

        InventoryAdjuster source = adjusters.of(dist.getSourceType());
        for (WarehouseDistributionDetails line : linesOf(distributionId)) {
            source.decrement(new StockAdjustment(
                    dist.getSourceId(), line.getProduct(), line.getPackaging(), line.getBatch(),
                    line.getIssueQuantity(), line, actor, now));
        }

        appendStatus(dist, DistributionStatus.PRODUCTS_DISPATCHED, actor, now);
    }

    @Override
    public void receive(Long distributionId, WarehouseDistributionReceiveRequest request, UserDetails user) {
        String actor = actorOf(user);
        LocalDateTime now = LocalDateTime.now();
        WarehouseDistribution dist = getOrThrow(distributionId);

        // Guard: only dispatched stock may be received — prevents receiving before
        // dispatch and receiving twice.
        requireStatus(distributionId, DistributionStatus.PRODUCTS_DISPATCHED, "receive");

        // Per-line receiver payloads keyed by the dispatched line id. A line that is
        // omitted from the request defaults to its issued quantity, zero damaged and
        // no remarks.
        Map<Long, WarehouseDistributionReceiveLineRequest> receiveByLine = receiveLinesById(request);

        List<WarehouseDistributionDetails> lines = linesOf(distributionId);
        InventoryAdjuster destination = adjusters.of(dist.getDestinationType());
        for (WarehouseDistributionDetails line : lines) {
            WarehouseDistributionReceiveLineRequest lr =
                    receiveByLine.get(line.getWarehouseDistributionDetailsId());
            long received = (lr != null && lr.getReceivedQuantity() != null)
                    ? lr.getReceivedQuantity() : line.getIssueQuantity();
            long damaged = (lr != null && lr.getDamagedQuantity() != null)
                    ? lr.getDamagedQuantity() : 0L;

            // A partial receipt is allowed, but you can never receive more than what
            // was dispatched.
            if (received > line.getIssueQuantity()) {
                throw new IllegalArgumentException(
                        "Received quantity (" + received + ") cannot exceed the issued quantity ("
                                + line.getIssueQuantity() + ") for line "
                                + line.getWarehouseDistributionDetailsId());
            }

            // Received plus damaged/not-received can account for at most the issued
            // quantity — anything beyond that is inconsistent.
            if (received + damaged > line.getIssueQuantity()) {
                throw new IllegalArgumentException(
                        "Received (" + received + ") plus damaged/not-received (" + damaged
                                + ") cannot exceed the issued quantity (" + line.getIssueQuantity()
                                + ") for line " + line.getWarehouseDistributionDetailsId());
            }

            // Persist the actual arrived quantity, the damaged/not-received quantity
            // and the receiver's remarks so they surface in the response and are
            // available for later reconciliation.
            line.setReceivedQuantity(received);
            line.setDamagedQuantity(damaged);
            if (lr != null) {
                line.setRemarks(lr.getRemarks());
            }
            line.setModifiedBy(actor);
            line.setModifiedAt(now);
            detailsRepository.save(line);

            destination.increment(new StockAdjustment(
                    dist.getDestinationId(), line.getProduct(), line.getPackaging(), line.getBatch(),
                    received, line, actor, now));
        }

        // Reject line ids in the request that don't belong to this distribution.
        receiveByLine.keySet().removeIf(id ->
                lines.stream().anyMatch(l -> l.getWarehouseDistributionDetailsId().equals(id)));
        if (!receiveByLine.isEmpty()) {
            throw new IllegalArgumentException(
                    "Received lines do not belong to distribution " + distributionId + ": "
                            + receiveByLine.keySet());
        }

        appendStatus(dist, DistributionStatus.STOCK_RECEIVED, actor, now);
    }

    /**
     * Builds a map of dispatched-line id -> the receiver's line payload (received
     * quantity, damaged/not-received quantity and remarks), validating that each
     * referenced line carries an id and non-negative quantities.
     */
    private Map<Long, WarehouseDistributionReceiveLineRequest> receiveLinesById(
            WarehouseDistributionReceiveRequest request) {
        Map<Long, WarehouseDistributionReceiveLineRequest> byLine = new HashMap<>();
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            return byLine;
        }
        for (WarehouseDistributionReceiveLineRequest lr : request.getLines()) {
            if (lr.getWarehouseDistributionDetailsId() == null) {
                throw new IllegalArgumentException(
                        "warehouseDistributionDetailsId is required for every received line");
            }
            if (lr.getReceivedQuantity() == null || lr.getReceivedQuantity() < 0) {
                throw new IllegalArgumentException("Received quantity must be zero or positive");
            }
            if (lr.getDamagedQuantity() != null && lr.getDamagedQuantity() < 0) {
                throw new IllegalArgumentException("Damaged/not-received quantity must be zero or positive");
            }
            byLine.put(lr.getWarehouseDistributionDetailsId(), lr);
        }
        return byLine;
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDistributionResponse getById(Long distributionId) {
        WarehouseDistribution dist = getOrThrow(distributionId);
        // Lines with product/packaging/batch fetched in one query (a JOIN FETCH, so no
        // N+1 on the nested associations).
        List<WarehouseDistributionDetails> lines =
                detailsRepository.findLinesWithProductGraph(distributionId);
        // Status history comes from the @OneToMany (ordered oldest-first by @OrderBy);
        // the current status is simply the last entry, so no separate query is needed.
        List<WarehouseDistributionStatus> statusHistory = dist.getStatuses();
        DistributionStatus currentStatus = statusHistory.isEmpty()
                ? null
                : statusHistory.getLast().getWarehouseDistributionStatus();
        return toResponse(dist, lines, statusHistory, currentStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDistributionSummaryResponse> getAll(UserDetails user) {
        String warehouseId = actingWarehouseId(user);
        // Outgoing (requested by this warehouse) + incoming (this warehouse is the
        // destination).
        return toSummaries(
                distributionRepository.findForWarehouse(
                        warehouseId, LocationType.WAREHOUSE, byAllocationDateDesc()),
                warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDistributionSummaryResponse> getBySource(UserDetails user) {
        // The acting location may be a warehouse OR a pharmacy, so use the resolved
        // location type rather than assuming WAREHOUSE.
        LocationContext ctx = locationContextResolver.resolve(user);
        // Only distributions shipped FROM this location (it is the source).
        return toSummaries(
                distributionRepository.findBySourceTypeAndSourceId(
                        ctx.getType(), ctx.getLocationId(), byAllocationDateDesc()),
                ctx.getLocationId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDistributionSummaryResponse> getByDestination(UserDetails user) {
        // The acting location may be a warehouse OR a pharmacy, so use the resolved
        // location type rather than assuming WAREHOUSE.
        LocationContext ctx = locationContextResolver.resolve(user);
        // Only distributions shipped TO this location (it is the destination).
        return toSummaries(
                distributionRepository.findByDestinationTypeAndDestinationId(
                        ctx.getType(), ctx.getLocationId(), byAllocationDateDesc()),
                ctx.getLocationId());
    }

    @Override
    @Transactional(readOnly = true)
    public String peekNextAllocationNo() {
        return allocationNoGenerator.generate();
    }

    /**
     * Turns a page of distributions into summary rows for {@code warehouseId}. The
     * line totals, latest status and store names are each fetched in one bulk query
     * scoped to these distributions, so there is no per-row (N+1) lookup.
     */
    private List<WarehouseDistributionSummaryResponse> toSummaries(
            List<WarehouseDistribution> distributions, String warehouseId) {
        if (distributions.isEmpty()) {
            return List.of();
        }

        List<Long> distributionIds = distributions.stream()
                .map(WarehouseDistribution::getWarehouseDistributionId)
                .toList();

        // Line totals (products count + issued quantity) per distribution — one
        // grouped query scoped to this page instead of iterating lines per row.
        Map<Long, WarehouseDistributionDetailsRepository.DistributionLineAggregate> aggByDist =
                detailsRepository.aggregateLinesByDistribution(distributionIds).stream()
                        .collect(Collectors.toMap(
                                WarehouseDistributionDetailsRepository.DistributionLineAggregate::getDistributionId,
                                agg -> agg));

        // Latest status per distribution — one query for this page.
        Map<Long, DistributionStatus> statusByDist =
                statusRepository.findLatestStatuses(distributionIds).stream()
                        .collect(Collectors.toMap(
                                s -> s.getWarehouseDistribution().getWarehouseDistributionId(),
                                WarehouseDistributionStatus::getWarehouseDistributionStatus));

        // Resolve both ends to a store name, fetching every referenced warehouse and
        // pharmacy in bulk rather than one lookup per row.
        Set<String> warehouseIds = new HashSet<>();
        Set<String> pharmacyIds = new HashSet<>();
        for (WarehouseDistribution d : distributions) {
            collectStoreId(d.getSourceType(), d.getSourceId(), warehouseIds, pharmacyIds);
            collectStoreId(d.getDestinationType(), d.getDestinationId(), warehouseIds, pharmacyIds);
        }
        Map<String, String> warehouseNames = warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getWarehouseName));
        Map<String, String> pharmacyNames = pharmacyRepository.findAllById(pharmacyIds).stream()
                .collect(Collectors.toMap(PharmacyDetails::getPharmacyId, PharmacyDetails::getPharmacyName));

        return distributions.stream()
                .map(d -> toSummary(d, warehouseId,
                        aggByDist.get(d.getWarehouseDistributionId()),
                        statusByDist.get(d.getWarehouseDistributionId()),
                        warehouseNames, pharmacyNames))
                .toList();
    }

    // The warehouse the acting user is operating as — the same id stored as
    // allocationRequestedBy at create time — so list screens never load the whole table.
    private String actingWarehouseId(UserDetails user) {
        return locationContextResolver.resolve(user).getLocationId();
    }

    private Sort byAllocationDateDesc() {
        return Sort.by(Sort.Direction.DESC, "allocationDate");
    }

    // ---- helpers -------------------------------------------------------------

    private String actorOf(UserDetails user) {
        return String.valueOf(user.getUserId());
    }

    /**
     * Business rules for a new allocation: endpoints exist, both ends belong to the
     * acting user's organization, a warehouse manager may only ship from their own
     * warehouse, a warehouse may only ship to pharmacies it serves (the pharmacy ->
     * warehouse link from step 3), and every line carries a positive quantity.
     */
    private void validateAllocation(WarehouseDistributionRequest request, UserDetails user) {

        if (request.getSourceType() == null
                || request.getSourceId() == null || request.getSourceId().isBlank()) {
            throw new IllegalArgumentException("Source type and id are required");
        }
        if (request.getDestinationType() == null
                || request.getDestinationId() == null || request.getDestinationId().isBlank()) {
            throw new IllegalArgumentException("Destination type and id are required");
        }
        if (request.getSourceType() == request.getDestinationType()
                && request.getSourceId().equals(request.getDestinationId())) {
            throw new IllegalArgumentException("Source and destination must be different");
        }

        // Both ends must belong to the acting user's organization.
        Long orgId = organizationService.getUserOrganization(user.getUserId()).getOrganizationId();
        if (!orgId.equals(organizationIdOf(request.getSourceType(), request.getSourceId()))) {
            throw new IllegalArgumentException("Source does not belong to your organization");
        }
        if (!orgId.equals(organizationIdOf(request.getDestinationType(), request.getDestinationId()))) {
            throw new IllegalArgumentException("Destination does not belong to your organization");
        }

        // A warehouse manager may only ship from a warehouse they are mapped to. This
        // only constrains a warehouse-sourced allocation — a pharmacy-to-pharmacy
        // transfer has no warehouse involved at all, so it is not subject to this rule
        // even when the acting user happens to also manage a warehouse.
        if (request.getSourceType() == LocationType.WAREHOUSE) {
            List<String> managedWarehouses = locationContextResolver.managedWarehouseIds(user);
            if (!managedWarehouses.isEmpty() && !managedWarehouses.contains(request.getSourceId())) {
                throw new IllegalArgumentException(
                        "You can only distribute from a warehouse you are mapped to");
            }
        }

        // Warehouse -> pharmacy: the pharmacy must be served by the source warehouse.
        if (request.getSourceType() == LocationType.WAREHOUSE
                && request.getDestinationType() == LocationType.PHARMACY) {
            PharmacyDetails dest = pharmacyRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pharmacy not found: " + request.getDestinationId()));
            if (dest.getWarehouse() == null
                    || !request.getSourceId().equals(dest.getWarehouse().getWarehouseId())) {
                throw new IllegalArgumentException(
                        "Pharmacy " + request.getDestinationId()
                                + " is not served by warehouse " + request.getSourceId());
            }
        }

        for (WarehouseDistributionLineRequest line : request.getLines()) {
            if (line.getIssueQuantity() == null || line.getIssueQuantity() <= 0) {
                throw new IllegalArgumentException("Issue quantity must be positive for every line");
            }
        }
    }

    private Long organizationIdOf(LocationType type, String id) {
        PharmacyOrganization org;
        if (type == LocationType.WAREHOUSE) {
            Warehouse warehouse = warehouseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
            org = warehouse.getOrganization();
        } else {
            PharmacyDetails pharmacy = pharmacyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found: " + id));
            org = pharmacy.getOrganization();
        }
        return org == null ? null : org.getOrganizationId();
    }

    private List<WarehouseDistributionDetails> linesOf(Long distributionId) {
        return detailsRepository.findByWarehouseDistribution_WarehouseDistributionId(distributionId);
    }

    /**
     * Throws unless the distribution's current status equals {@code expected}.
     */
    private void requireStatus(Long distributionId, DistributionStatus expected, String action) {
        DistributionStatus current = currentStatusOrNull(distributionId);
        if (current != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " distribution " + distributionId
                            + ": expected status " + expected + " but was " + current);
        }
    }

    private DistributionStatus currentStatusOrNull(Long distributionId) {
        return statusRepository
                .findFirstByWarehouseDistribution_WarehouseDistributionIdOrderByWarehouseDistributionStatusIdDesc(distributionId)
                .map(WarehouseDistributionStatus::getWarehouseDistributionStatus)
                .orElse(null);
    }

    private void appendStatus(WarehouseDistribution dist, DistributionStatus status,
                              String actor, LocalDateTime at) {
        WarehouseDistributionStatus row = new WarehouseDistributionStatus();
        row.setWarehouseDistribution(dist);
        row.setWarehouseDistributionStatus(status);
        row.setCreatedBy(actor);
        row.setCreatedAt(at);
        statusRepository.save(row);
    }

    private WarehouseDistribution getOrThrow(Long id) {
        return distributionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution not found: " + id));
    }

    private WarehouseDistributionResponse toResponse(WarehouseDistribution dist,
                                                     List<WarehouseDistributionDetails> lines,
                                                     List<WarehouseDistributionStatus> statusHistory,
                                                     DistributionStatus currentStatus) {
        WarehouseDistributionResponse res = new WarehouseDistributionResponse();
        res.setWarehouseDistributionId(dist.getWarehouseDistributionId());
        res.setAllocationMode(dist.getAllocationMode());
        res.setAllocationNo(dist.getAllocationNo());
        res.setAllocationDate(dist.getAllocationDate());
        res.setDistributionType(dist.getDistributionType());
        res.setReference(dist.getReference());
        res.setRemarks(dist.getRemarks());
        res.setSourceType(dist.getSourceType());
        res.setSourceId(dist.getSourceId());
        res.setSourceName(resolveStoreName(dist.getSourceType(), dist.getSourceId()));
        res.setDestinationType(dist.getDestinationType());
        res.setDestinationId(dist.getDestinationId());
        res.setDestinationName(resolveStoreName(dist.getDestinationType(), dist.getDestinationId()));
        res.setAllocationRequestedBy(dist.getAllocationRequestedBy());
        res.setCurrentStatus(currentStatus);
        res.setCreatedBy(dist.getCreatedBy());
        res.setCreatedAt(dist.getCreatedAt());
        res.setLines(lines.stream().map(this::toLineResponse).toList());
        res.setStatuses(statusHistory.stream().map(this::toStatusResponse).toList());
        return res;
    }

    private WarehouseDistributionStatusResponse toStatusResponse(WarehouseDistributionStatus status) {
        WarehouseDistributionStatusResponse dto = new WarehouseDistributionStatusResponse();
        dto.setWarehouseDistributionStatusId(status.getWarehouseDistributionStatusId());
        dto.setStatus(status.getWarehouseDistributionStatus());
        dto.setCreatedBy(status.getCreatedBy());
        dto.setCreatedAt(status.getCreatedAt());
        return dto;
    }

    private WarehouseDistributionSummaryResponse toSummary(
            WarehouseDistribution d,
            String warehouseId,
            WarehouseDistributionDetailsRepository.DistributionLineAggregate agg,
            DistributionStatus currentStatus,
            Map<String, String> warehouseNames,
            Map<String, String> pharmacyNames) {
        WarehouseDistributionSummaryResponse row = new WarehouseDistributionSummaryResponse();
        row.setWarehouseDistributionId(d.getWarehouseDistributionId());
        row.setAllocationNo(d.getAllocationNo());
        // Incoming when this warehouse is the destination; otherwise it's the one
        // that requested/ships the stock.
        boolean incoming = d.getDestinationType() == LocationType.WAREHOUSE
                && warehouseId.equals(d.getDestinationId());
        row.setDirection(incoming ? "INCOMING" : "OUTGOING");
        row.setFromType(d.getSourceType());
        row.setFromId(d.getSourceId());
        row.setFromStore(storeName(d.getSourceType(), d.getSourceId(), warehouseNames, pharmacyNames));
        row.setToType(d.getDestinationType());
        row.setToId(d.getDestinationId());
        row.setToStore(storeName(d.getDestinationType(), d.getDestinationId(), warehouseNames, pharmacyNames));
        row.setProductsCount(agg == null ? 0L : agg.getProductsCount());
        row.setTotalQuantity(agg == null ? 0L : agg.getTotalQuantity());
        row.setCurrentStatus(currentStatus);
        row.setAllocationDate(d.getAllocationDate());
        return row;
    }

    // Bucket a store id into the warehouse or pharmacy set by its location type, so
    // names can be resolved in two bulk lookups.
    private void collectStoreId(LocationType type, String id,
                                Set<String> warehouseIds, Set<String> pharmacyIds) {
        if (id == null || type == null) {
            return;
        }
        if (type == LocationType.WAREHOUSE) {
            warehouseIds.add(id);
        } else if (type == LocationType.PHARMACY) {
            pharmacyIds.add(id);
        }
    }

    private String storeName(LocationType type, String id,
                             Map<String, String> warehouseNames, Map<String, String> pharmacyNames) {
        if (id == null || type == null) {
            return null;
        }
        return type == LocationType.WAREHOUSE ? warehouseNames.get(id) : pharmacyNames.get(id);
    }

    // Resolve a single store's display name for the detail view (one lookup each for
    // source/destination — fine for a single distribution, unlike the bulk path used by
    // the list screen).
    private String resolveStoreName(LocationType type, String id) {
        if (id == null || type == null) {
            return null;
        }
        if (type == LocationType.WAREHOUSE) {
            return warehouseRepository.findById(id)
                    .map(Warehouse::getWarehouseName)
                    .orElse(null);
        }
        return pharmacyRepository.findById(id)
                .map(PharmacyDetails::getPharmacyName)
                .orElse(null);
    }

    private WarehouseDistributionLineResponse toLineResponse(WarehouseDistributionDetails line) {
        WarehouseDistributionLineResponse dto = new WarehouseDistributionLineResponse();
        dto.setWarehouseDistributionDetailsId(line.getWarehouseDistributionDetailsId());
        dto.setIssueQuantity(line.getIssueQuantity());
        dto.setReceivedQuantity(line.getReceivedQuantity());
        dto.setDamagedQuantity(line.getDamagedQuantity());
        dto.setRemarks(line.getRemarks());

        ProductDetails product = line.getProduct();
        if (product != null) {
            dto.setProductId(product.getProductId());
            WarehouseDistributionLineResponse.ProductInfo p =
                    new WarehouseDistributionLineResponse.ProductInfo();
            p.setProductId(product.getProductId());
            p.setProductName(product.getProductName());
            p.setBrandName(product.getBrandName());
            p.setHsnNo(product.getHsnNo());
            p.setGstPercentage(product.getGstPercentage());
            dto.setProduct(p);
        }

        PackagingDetails packaging = line.getPackaging();
        if (packaging != null) {
            dto.setPackagingId(packaging.getPackagingId());
            WarehouseDistributionLineResponse.PackagingInfo pk =
                    new WarehouseDistributionLineResponse.PackagingInfo();
            pk.setPackagingId(packaging.getPackagingId());
            pk.setPurchaseUnit(packaging.getPurchaseUnit());
            pk.setPurchaseUnitContains(packaging.getPurchaseUnitContains());
            dto.setPackaging(pk);
        }

        BatchDetails batch = line.getBatch();
        if (batch != null) {
            dto.setBatchId(batch.getBatchId());
            WarehouseDistributionLineResponse.BatchInfo b =
                    new WarehouseDistributionLineResponse.BatchInfo();
            b.setBatchId(batch.getBatchId());
            b.setBatchNumber(batch.getBatchNumber());
            b.setManufacturingDate(batch.getManufacturingDate());
            b.setExpiryDate(batch.getExpiryDate());
            b.setMrp(batch.getMrp());
            b.setSellingPrice(batch.getSellingPrice());
            b.setPurchasePrice(batch.getPurchasePrice());
            b.setRackLocation(batch.getRackLocation());
            dto.setBatch(b);
        }
        return dto;
    }
}
