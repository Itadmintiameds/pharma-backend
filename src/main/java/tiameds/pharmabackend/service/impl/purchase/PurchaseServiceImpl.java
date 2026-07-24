package tiameds.pharmabackend.service.impl.purchase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.mapper.purchase.PurchaseMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.supplier.SupplierMasterRepository;
import tiameds.pharmabackend.service.purchase.PurchaseService;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final ProductDetailsRepository pharmaProductDetailsRepository;
    private final BatchDetailsRepository pharmaBatchDetailsRepository;

    @Override
    public PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                purchaseDto.getPharmacyId(),
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        Purchase purchase = PurchaseMapper.toEntity(purchaseDto);

        SupplierMaster supplier = supplierMasterRepository.findById(purchaseDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        purchase.setSupplier(supplier);


        if (purchase.getPurchaseDetails() != null) {

            for (int i = 0; i < purchase.getPurchaseDetails().size(); i++) {

                PurchaseDetails detail = purchase.getPurchaseDetails().get(i);
                var dto = purchaseDto.getPurchaseDetails().get(i);

                ProductDetails product = pharmaProductDetailsRepository
                        .findById(dto.getProductId())
                        .orElseThrow(() -> new RuntimeException(
                                "Product not found: " + dto.getProductId()));

                BatchDetails batch = pharmaBatchDetailsRepository
                        .findById(dto.getBatchId())
                        .orElseThrow(() -> new RuntimeException(
                                "Batch not found: " + dto.getBatchId()));

                Long currentStock = batch.getStockQuantity() != null
                        ? batch.getStockQuantity()
                        : 0L;

                Long purchaseQty = detail.getPurchaseQuantity() != null
                        ? detail.getPurchaseQuantity()
                        : 0L;

                batch.setStockQuantity(currentStock + purchaseQty);

                detail.setProduct(product);
                detail.setBatch(batch);
            }
        }

        purchase.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        purchase.setCreatedAt(LocalDateTime.now());
        purchase.setModifiedBy(null);
        purchase.setModifiedAt(null);

        if (purchase.getPurchaseDetails() != null) {
            for (PurchaseDetails detail : purchase.getPurchaseDetails()) {

                detail.setPurchase(purchase);
                detail.setCreatedBy(String.valueOf(persistentUser.getUserId()));
                detail.setCreatedAt(LocalDateTime.now());
                detail.setModifiedBy(null);
                detail.setModifiedAt(null);
            }
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);

        return PurchaseMapper.toDto(savedPurchase);
    }
}