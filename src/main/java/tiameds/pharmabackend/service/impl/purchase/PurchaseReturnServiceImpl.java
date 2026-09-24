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
import tiameds.pharmabackend.enums.TransactionType;
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
import java.util.List;
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

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

        decrementInventoryForReturn(purchaseReturn, LocationType.PHARMACY, pharmacyId, actor, now);

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

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

        decrementInventoryForReturn(purchaseReturn, LocationType.WAREHOUSE, warehouseId, actor, now);

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
        if (location.isWarehouse()) {

            if (!location.getLocationId().equals(purchaseReturn.getWarehouseId())) {
                throw new RuntimeException("This purchase return does not belong to your warehouse.");
            }

            return PurchaseReturnMapper.toDto(purchaseReturn);
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

        return PurchaseReturnMapper.toDto(purchaseReturn);
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

            BatchDetails batch = detail.getBatch();
            PackagingDetails packaging = batch.getPackagingDetails();

            Long returnQty = detail.getPurchaseReturnQuantity() != null
                    ? detail.getPurchaseReturnQuantity()
                    : 0L;

            Long purchaseUnitContains = (packaging != null && packaging.getPurchaseUnitContains() != null)
                    ? packaging.getPurchaseUnitContains()
                    : 1L;

            // Stock is tracked in smallest units — same conversion used when the
            // purchase originally brought this batch in.
            long stockQty = returnQty * purchaseUnitContains;

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
