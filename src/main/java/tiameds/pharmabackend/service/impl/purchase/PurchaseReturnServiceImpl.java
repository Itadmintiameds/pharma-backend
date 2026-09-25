package tiameds.pharmabackend.service.impl.purchase;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseReturn;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.purchase.PurchaseReturnDetailsMapper;
import tiameds.pharmabackend.mapper.purchase.PurchaseReturnMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseReturnRepository;
import tiameds.pharmabackend.service.impl.warehouse.stock.InventoryAdjusters;
import tiameds.pharmabackend.service.purchase.PurchaseReturnService;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseReturnServiceImpl implements PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final ProductDetailsRepository pharmaProductDetailsRepository;
    private final BatchDetailsRepository pharmaBatchDetailsRepository;
    private final LocationContextResolver locationContextResolver;
    private final InventoryAdjusters adjusters;

    @Override
    public PurchaseReturnDto createPurchaseReturn(PurchaseReturnDto purchaseReturnDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Same decision point as a purchase: a warehouse manager returns against
        // their warehouse's purchases, everyone else against their selected pharmacy.
        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return createWarehousePurchaseReturn(purchaseReturnDto, persistentUser, location.getLocationId());
        }

        return createPharmacyPurchaseReturn(purchaseReturnDto, persistentUser, location.getLocationId());
    }

    private PurchaseReturnDto createPharmacyPurchaseReturn(
            PurchaseReturnDto purchaseReturnDto,
            UserDetails persistentUser,
            String pharmacyId) {

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        Purchase purchase = purchaseRepository.findById(purchaseReturnDto.getPurchaseId())
                .orElseThrow(() -> new RuntimeException(
                        "Purchase not found: " + purchaseReturnDto.getPurchaseId()));

        if (!pharmacyId.equals(purchase.getPharmacyId())) {
            throw new RuntimeException("This purchase does not belong to your pharmacy.");
        }

        PurchaseReturn purchaseReturn = PurchaseReturnMapper.toEntity(purchaseReturnDto);

        purchaseReturn.setPurchase(purchase);
        purchaseReturn.setPharmacyId(pharmacyId);
        purchaseReturn.setWarehouseId(null);
        purchaseReturn.setReturnNo(generateReturnNo(pharmacyId));

        PurchaseReturnStatus status = resolveCreateStatus(purchaseReturnDto.getStatus());
        purchaseReturn.setStatus(status);

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

        // A draft is only a saved document: no stock leaves and no audit row is
        // written until it is confirmed.
        if (status == PurchaseReturnStatus.CONFIRMED) {
            decrementInventoryForReturn(purchaseReturn, LocationType.PHARMACY, pharmacyId, actor, now);
        }

        PurchaseReturn savedPurchaseReturn = purchaseReturnRepository.save(purchaseReturn);

        return PurchaseReturnMapper.toDto(savedPurchaseReturn);
    }

    private PurchaseReturnDto createWarehousePurchaseReturn(
            PurchaseReturnDto purchaseReturnDto,
            UserDetails persistentUser,
            String warehouseId) {

        Purchase purchase = purchaseRepository.findById(purchaseReturnDto.getPurchaseId())
                .orElseThrow(() -> new RuntimeException(
                        "Purchase not found: " + purchaseReturnDto.getPurchaseId()));

        if (!warehouseId.equals(purchase.getWarehouseId())) {
            throw new RuntimeException("This purchase does not belong to your warehouse.");
        }

        PurchaseReturn purchaseReturn = PurchaseReturnMapper.toEntity(purchaseReturnDto);

        purchaseReturn.setPurchase(purchase);
        purchaseReturn.setPharmacyId(null);
        purchaseReturn.setWarehouseId(warehouseId);
        purchaseReturn.setReturnNo(generateReturnNoForWarehouse(warehouseId));

        PurchaseReturnStatus status = resolveCreateStatus(purchaseReturnDto.getStatus());
        purchaseReturn.setStatus(status);

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

        // A draft is only a saved document: no stock leaves and no audit row is
        // written until it is confirmed.
        if (status == PurchaseReturnStatus.CONFIRMED) {
            decrementInventoryForReturn(purchaseReturn, LocationType.WAREHOUSE, warehouseId, actor, now);
        }

        PurchaseReturn savedPurchaseReturn = purchaseReturnRepository.save(purchaseReturn);

        return PurchaseReturnMapper.toDto(savedPurchaseReturn);
    }

    @Override
    public List<PurchaseReturnDto> getAllPurchaseReturns(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return purchaseReturnRepository.findByWarehouseId(location.getLocationId())
                    .stream()
                    .map(PurchaseReturnMapper::toDto)
                    .collect(Collectors.toList());
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return purchaseReturnRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(PurchaseReturnMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PurchaseReturnDto getPurchaseReturnById(Long purchaseReturnId, UserDetails user) {

        if (purchaseReturnId == null) {
            throw new RuntimeException("Purchase return id is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase return not found: " + purchaseReturnId));

        // A return is only visible from the location it was raised at.
        assertReturnBelongsToLocation(purchaseReturn, location, persistentUser);

        return PurchaseReturnMapper.toDto(purchaseReturn);
    }

    @Override
    public PurchaseReturnDto updatePurchaseReturn(
            Long purchaseReturnId,
            PurchaseReturnDto purchaseReturnDto,
            UserDetails user) {

        if (purchaseReturnId == null) {
            throw new RuntimeException("Purchase return id is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase return not found: " + purchaseReturnId));

        assertReturnBelongsToLocation(purchaseReturn, location, persistentUser);

        // Only a draft is still editable. Once a return is confirmed its stock has
        // already left, and once cancelled it is closed — editing either would put
        // the document and the inventory out of step.
        if (purchaseReturn.getStatus() != PurchaseReturnStatus.DRAFT) {
            throw new RuntimeException(
                    "Only a draft purchase return can be edited. This one is "
                            + purchaseReturn.getStatus() + ".");
        }

        PurchaseReturnStatus requestedStatus = purchaseReturnDto.getStatus() != null
                ? purchaseReturnDto.getStatus()
                : PurchaseReturnStatus.DRAFT;

        if (requestedStatus == PurchaseReturnStatus.CANCELLED) {
            throw new RuntimeException(
                    "A purchase return cannot be cancelled through this endpoint.");
        }

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        purchaseReturn.setTotalGrossAmount(purchaseReturnDto.getTotalGrossAmount());
        purchaseReturn.setTotalGstAmount(purchaseReturnDto.getTotalGstAmount());
        purchaseReturn.setTotalNetAmount(purchaseReturnDto.getTotalNetAmount());
        purchaseReturn.setEditReason(purchaseReturnDto.getEditReason());
        purchaseReturn.setModifiedBy(actor);
        purchaseReturn.setModifiedAt(now);

        replaceReturnDetails(purchaseReturn, purchaseReturnDto, actor, now);

        purchaseReturn.setStatus(requestedStatus);

        // Confirming is the point the goods count as gone: this is the one place
        // an edit moves stock, and it can only happen on the DRAFT -> CONFIRMED
        // step guarded above, so a return can never be shipped out twice.
        if (requestedStatus == PurchaseReturnStatus.CONFIRMED) {

            LocationType locationType = location.isWarehouse()
                    ? LocationType.WAREHOUSE
                    : LocationType.PHARMACY;

            decrementInventoryForReturn(
                    purchaseReturn,
                    locationType,
                    location.getLocationId(),
                    actor,
                    now);
        }

        PurchaseReturn savedPurchaseReturn = purchaseReturnRepository.save(purchaseReturn);

        return PurchaseReturnMapper.toDto(savedPurchaseReturn);
    }

    @Override
    public PurchaseReturnDto editPurchaseReturn(
            Long purchaseReturnId,
            PurchaseReturnDto purchaseReturnDto,
            UserDetails user) {

        if (purchaseReturnId == null) {
            throw new RuntimeException("Purchase return id is required");
        }

        if (purchaseReturnDto.getEditReason() == null || purchaseReturnDto.getEditReason().isBlank()) {
            throw new RuntimeException("Edit reason is required");
        }

        if (purchaseReturnDto.getPurchaseReturnDetails() == null
                || purchaseReturnDto.getPurchaseReturnDetails().isEmpty()) {
            throw new RuntimeException("At least one purchase return line is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        PurchaseReturn purchaseReturn = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase return not found: " + purchaseReturnId));

        assertReturnBelongsToLocation(purchaseReturn, location, persistentUser);

        if (purchaseReturn.getStatus() == PurchaseReturnStatus.CANCELLED) {
            throw new RuntimeException("A cancelled purchase return cannot be edited.");
        }

        // Only a confirmed return has already taken stock out, so only then does
        // a quantity change have to be squared against inventory.
        boolean adjustStock = purchaseReturn.getStatus() == PurchaseReturnStatus.CONFIRMED;

        LocationType locationType = purchaseReturn.getWarehouseId() != null
                ? LocationType.WAREHOUSE
                : LocationType.PHARMACY;

        String locationId = purchaseReturn.getWarehouseId() != null
                ? purchaseReturn.getWarehouseId()
                : purchaseReturn.getPharmacyId();

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        Map<Long, PurchaseReturnDetails> currentById = purchaseReturn.getPurchaseReturnDetails()
                .stream()
                .filter(PurchaseReturnDetails::isCurrent)
                .collect(Collectors.toMap(PurchaseReturnDetails::getPurchaseReturnDetailId, d -> d));

        Set<Long> seen = new HashSet<>();
        List<PurchaseReturnDetails> revisions = new ArrayList<>();

        for (var lineDto : purchaseReturnDto.getPurchaseReturnDetails()) {

            Long detailId = lineDto.getPurchaseReturnDetailId();

            if (detailId == null) {
                throw new RuntimeException("purchaseReturnDetailId is required on every line");
            }

            if (!seen.add(detailId)) {
                throw new RuntimeException("Purchase return line " + detailId + " is sent more than once");
            }

            // Only the latest revision can be edited — editing a superseded row
            // would fork the line's history.
            PurchaseReturnDetails current = currentById.get(detailId);

            if (current == null) {
                throw new RuntimeException(
                        "Purchase return line " + detailId
                                + " is not a current line of this return");
            }

            // A field left out of the payload keeps its current value.
            Long newQty = lineDto.getPurchaseReturnQuantity() != null
                    ? lineDto.getPurchaseReturnQuantity()
                    : current.getPurchaseReturnQuantity();

            String newFreeQty = lineDto.getFreeReturnQuantity() != null
                    ? lineDto.getFreeReturnQuantity()
                    : current.getFreeReturnQuantity();

            if (newQty != null && newQty < 0) {
                throw new RuntimeException("Return quantity cannot be negative on line " + detailId);
            }

            if (Objects.equals(newQty, current.getPurchaseReturnQuantity())
                    && Objects.equals(newFreeQty, current.getFreeReturnQuantity())) {
                continue;
            }

            PurchaseReturnDetails revision = newRevision(current, newQty, newFreeQty, actor, now);

            current.setIsActive(false);
            current.setModifiedBy(actor);
            current.setModifiedAt(now);

            if (adjustStock) {
                adjustStockForRevision(current, revision, locationType, locationId, actor, now);
            }

            revisions.add(revision);
        }

        if (revisions.isEmpty()) {
            throw new RuntimeException("No quantity changes to save");
        }

        purchaseReturn.getPurchaseReturnDetails().addAll(revisions);

        // The header row is kept as is apart from the reason for this edit.
        purchaseReturn.setEditReason(purchaseReturnDto.getEditReason());
        purchaseReturn.setModifiedBy(actor);
        purchaseReturn.setModifiedAt(now);

        PurchaseReturn savedPurchaseReturn = purchaseReturnRepository.save(purchaseReturn);

        return PurchaseReturnMapper.toDto(savedPurchaseReturn);
    }

    // Copies a line into its next revision, changing only the two quantities.
    private PurchaseReturnDetails newRevision(
            PurchaseReturnDetails current,
            Long newQty,
            String newFreeQty,
            String actor,
            LocalDateTime now) {

        PurchaseReturnDetails revision = new PurchaseReturnDetails();

        revision.setPurchaseReturn(current.getPurchaseReturn());
        revision.setProduct(current.getProduct());
        revision.setBatch(current.getBatch());
        revision.setPurchaseReturnQuantity(newQty);
        revision.setFreeReturnQuantity(newFreeQty);
        revision.setReturnReason(current.getReturnReason());
        revision.setGrossAmount(current.getGrossAmount());
        revision.setGstAmount(current.getGstAmount());
        revision.setNetAmount(current.getNetAmount());
        revision.setRevisionNo(current.currentRevisionNo() + 1);
        revision.setIsActive(true);
        revision.setPreviousDetailId(current.getPurchaseReturnDetailId());
        revision.setCreatedBy(actor);
        revision.setCreatedAt(now);
        revision.setModifiedBy(null);
        revision.setModifiedAt(null);

        return revision;
    }

    // On a confirmed return the old quantity has already left stock, so only the
    // difference moves: returning more takes more out, returning less puts the
    // surplus back.
    private void adjustStockForRevision(
            PurchaseReturnDetails previous,
            PurchaseReturnDetails revision,
            LocationType locationType,
            String locationId,
            String actor,
            LocalDateTime now) {

        BatchDetails batch = revision.getBatch();
        PackagingDetails packaging = batch.getPackagingDetails();

        long delta = toStockUnits(packaging, revision.getPurchaseReturnQuantity())
                - toStockUnits(packaging, previous.getPurchaseReturnQuantity());

        if (delta == 0) {
            return;
        }

        StockAdjustment adjustment = new StockAdjustment(
                locationId,
                revision.getProduct(),
                packaging,
                batch,
                Math.abs(delta),
                TransactionType.PURCHASE_RETURN,
                null,
                null,
                actor,
                now);

        InventoryAdjuster adjuster = adjusters.of(locationType);

        if (delta > 0) {
            adjuster.decrement(adjustment);
        } else {
            adjuster.increment(adjustment);
        }
    }

    // Stock is tracked in smallest units — same conversion used when the
    // purchase originally brought this batch in.
    private long toStockUnits(PackagingDetails packaging, Long purchaseQty) {

        long qty = purchaseQty != null ? purchaseQty : 0L;

        long purchaseUnitContains = (packaging != null && packaging.getPurchaseUnitContains() != null)
                ? packaging.getPurchaseUnitContains()
                : 1L;

        return qty * purchaseUnitContains;
    }

    // A return created with no status is a draft: the safe reading of an absent
    // value is "not yet committed", never "move the stock".
    private PurchaseReturnStatus resolveCreateStatus(PurchaseReturnStatus requested) {

        if (requested == null) {
            return PurchaseReturnStatus.DRAFT;
        }

        if (requested == PurchaseReturnStatus.CANCELLED) {
            throw new RuntimeException("A purchase return cannot be created as CANCELLED.");
        }

        return requested;
    }

    // Throws unless the return was raised at the caller's current location.
    private void assertReturnBelongsToLocation(
            PurchaseReturn purchaseReturn,
            LocationContext location,
            UserDetails persistentUser) {

        if (location.isWarehouse()) {

            if (!location.getLocationId().equals(purchaseReturn.getWarehouseId())) {
                throw new RuntimeException("This purchase return does not belong to your warehouse.");
            }

            return;
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        if (!pharmacyId.equals(purchaseReturn.getPharmacyId())) {
            throw new RuntimeException("This purchase return does not belong to your pharmacy.");
        }
    }

    // Swaps a draft's lines for the ones in the payload. orphanRemoval on the
    // association deletes the replaced rows, so the draft always matches what was
    // last sent rather than accumulating lines across edits.
    private void replaceReturnDetails(
            PurchaseReturn purchaseReturn,
            PurchaseReturnDto purchaseReturnDto,
            String actor,
            LocalDateTime now) {

        purchaseReturn.getPurchaseReturnDetails().clear();

        if (purchaseReturnDto.getPurchaseReturnDetails() == null) {
            return;
        }

        for (var detailsDto : purchaseReturnDto.getPurchaseReturnDetails()) {

            PurchaseReturnDetails detail = PurchaseReturnDetailsMapper.toEntity(detailsDto);

            detail.setPurchaseReturn(purchaseReturn);
            detail.setCreatedBy(actor);
            detail.setCreatedAt(now);
            detail.setModifiedBy(null);
            detail.setModifiedAt(null);

            purchaseReturn.getPurchaseReturnDetails().add(detail);
        }

        // Same index-aligned product/batch resolution the create path uses.
        resolveReturnDetails(purchaseReturn, purchaseReturnDto);
    }

    // Resolves the managed product/batch for each return line, matching the
    // pattern PurchaseServiceImpl uses when a purchase line is created.
    private void resolveReturnDetails(PurchaseReturn purchaseReturn, PurchaseReturnDto purchaseReturnDto) {

        if (purchaseReturn.getPurchaseReturnDetails() == null) {
            return;
        }

        for (int i = 0; i < purchaseReturn.getPurchaseReturnDetails().size(); i++) {

            PurchaseReturnDetails detail = purchaseReturn.getPurchaseReturnDetails().get(i);
            var dto = purchaseReturnDto.getPurchaseReturnDetails().get(i);

            ProductDetails product = pharmaProductDetailsRepository
                    .findById(dto.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found: " + dto.getProductId()));

            BatchDetails batch = pharmaBatchDetailsRepository
                    .findById(dto.getBatchId())
                    .orElseThrow(() ->
                            new RuntimeException("Batch not found: " + dto.getBatchId()));

            detail.setProduct(product);
            detail.setBatch(batch);
        }
    }

    // A purchase return sends stock back to the supplier — an OUT movement against
    // whichever location (pharmacy/warehouse) the return was raised at. Reuses the
    // same InventoryAdjuster the purchase flow uses to bring stock IN, so the
    // sufficiency check and pharma_inventory_audit / pharma_warehouse_inventory_audit
    // row are written the same way, just tagged PURCHASE_RETURN / OUT.
    private void decrementInventoryForReturn(
            PurchaseReturn purchaseReturn,
            LocationType locationType,
            String locationId,
            String actor,
            LocalDateTime now) {

        if (purchaseReturn.getPurchaseReturnDetails() == null) {
            return;
        }

        InventoryAdjuster adjuster = adjusters.of(locationType);

        for (PurchaseReturnDetails detail : purchaseReturn.getPurchaseReturnDetails()) {

            // Superseded revisions are history, not stock to move.
            if (!detail.isCurrent()) {
                continue;
            }

            BatchDetails batch = detail.getBatch();
            PackagingDetails packaging = batch.getPackagingDetails();

            long stockQty = toStockUnits(packaging, detail.getPurchaseReturnQuantity());

            adjuster.decrement(new StockAdjustment(
                    locationId,
                    detail.getProduct(),
                    packaging,
                    batch,
                    stockQty,
                    TransactionType.PURCHASE_RETURN,
                    null,   // no distribution line for a purchase return
                    null,   // no purchase line to trace back to — this is a return, not a purchase
                    actor,
                    now));
        }
    }

    private String generateReturnNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "PR-" + year + "-";

        List<String> latest = purchaseReturnRepository.findLatestReturnNo(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestReturnNo = latest.get(0);

            String numberPart = latestReturnNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private String generateReturnNoForWarehouse(String warehouseId) {

        int year = LocalDate.now().getYear();
        String prefix = "PR-" + year + "-";

        List<String> latest = purchaseReturnRepository.findLatestReturnNoByWarehouse(
                prefix,
                warehouseId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestReturnNo = latest.get(0);

            String numberPart = latestReturnNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private void stampAuditFields(PurchaseReturn purchaseReturn, String actor, LocalDateTime now) {

        purchaseReturn.setPurchaseReturnDate(now);
        purchaseReturn.setCreatedBy(actor);
        purchaseReturn.setCreatedAt(now);
        purchaseReturn.setModifiedBy(null);
        purchaseReturn.setModifiedAt(null);

        if (purchaseReturn.getPurchaseReturnDetails() != null) {

            for (PurchaseReturnDetails detail : purchaseReturn.getPurchaseReturnDetails()) {

                detail.setPurchaseReturn(purchaseReturn);
                detail.setCreatedBy(actor);
                detail.setCreatedAt(now);
                detail.setModifiedBy(null);
                detail.setModifiedAt(null);
            }
        }
    }
}
