package tiameds.pharmabackend.service.impl.purchase;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseReturn;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;
import tiameds.pharmabackend.mapper.purchase.PurchaseReturnMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseReturnRepository;
import tiameds.pharmabackend.service.purchase.PurchaseReturnService;

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

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

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

        resolveReturnDetails(purchaseReturn, purchaseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(purchaseReturn, actor, now);

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

    private void stampAuditFields(PurchaseReturn purchaseReturn, String actor, LocalDateTime now) {

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
