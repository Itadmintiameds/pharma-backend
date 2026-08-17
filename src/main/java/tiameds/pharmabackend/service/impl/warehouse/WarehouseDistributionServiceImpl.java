package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionLineRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionLineResponse;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionResponse;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
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
import java.util.List;

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
    public void receive(Long distributionId, UserDetails user) {
        String actor = actorOf(user);
        LocalDateTime now = LocalDateTime.now();
        WarehouseDistribution dist = getOrThrow(distributionId);

        // Guard: only dispatched stock may be received — prevents receiving before
        // dispatch and receiving twice.
        requireStatus(distributionId, DistributionStatus.PRODUCTS_DISPATCHED, "receive");

        // TODO (#3): accept a per-line received quantity from the request for partial
        // receipts / rejection (currently defaults to the issued quantity).
        InventoryAdjuster destination = adjusters.of(dist.getDestinationType());
        for (WarehouseDistributionDetails line : linesOf(distributionId)) {
            long received = line.getReceivedQuantity() != null
                    ? line.getReceivedQuantity()
                    : line.getIssueQuantity();
            destination.increment(new StockAdjustment(
                    dist.getDestinationId(), line.getProduct(), line.getPackaging(), line.getBatch(),
                    received, line, actor, now));
        }

        appendStatus(dist, DistributionStatus.STOCK_RECEIVED, actor, now);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDistributionResponse getById(Long distributionId) {
        WarehouseDistribution dist = getOrThrow(distributionId);
        return toResponse(dist, linesOf(distributionId), currentStatusOrNull(distributionId));
    }

    @Override
    @Transactional(readOnly = true)
    public String peekNextAllocationNo() {
        return allocationNoGenerator.generate();
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

        // A warehouse manager may only ship from a warehouse they are mapped to.
        List<String> managedWarehouses = locationContextResolver.managedWarehouseIds(user);
        if (!managedWarehouses.isEmpty()) {
            if (request.getSourceType() != LocationType.WAREHOUSE
                    || !managedWarehouses.contains(request.getSourceId())) {
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
        res.setDestinationType(dist.getDestinationType());
        res.setDestinationId(dist.getDestinationId());
        res.setAllocationRequestedBy(dist.getAllocationRequestedBy());
        res.setCurrentStatus(currentStatus);
        res.setCreatedBy(dist.getCreatedBy());
        res.setCreatedAt(dist.getCreatedAt());
        res.setLines(lines.stream().map(this::toLineResponse).toList());
        return res;
    }

    private WarehouseDistributionLineResponse toLineResponse(WarehouseDistributionDetails line) {
        WarehouseDistributionLineResponse dto = new WarehouseDistributionLineResponse();
        dto.setWarehouseDistributionDetailsId(line.getWarehouseDistributionDetailsId());
        dto.setProductId(line.getProduct().getProductId());
        dto.setPackagingId(line.getPackaging().getPackagingId());
        dto.setBatchId(line.getBatch().getBatchId());
        dto.setIssueQuantity(line.getIssueQuantity());
        dto.setReceivedQuantity(line.getReceivedQuantity());
        return dto;
    }
}
