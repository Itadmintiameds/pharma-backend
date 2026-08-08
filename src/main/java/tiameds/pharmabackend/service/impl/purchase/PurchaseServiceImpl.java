package tiameds.pharmabackend.service.impl.purchase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.purchase.PurchaseMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryAuditRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.repository.purchase.PurchaseRepository;
import tiameds.pharmabackend.repository.supplier.SupplierMasterRepository;
import tiameds.pharmabackend.service.purchase.PurchaseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

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
    private final InventoryRepository inventoryRepository;
    private final InventoryAuditRepository inventoryAuditRepository;
    private final CurrentPharmacyContext pharmacyContext;


    @Override
    public PurchaseDto createPurchase(PurchaseDto purchaseDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        PharmacyDetails pharmacy = pharmacyDetailsRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Purchase purchase = PurchaseMapper.toEntity(purchaseDto);

        purchase.setPharmacyId(pharmacyId);

        SupplierMaster supplier = supplierMasterRepository.findById(purchaseDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        purchase.setSupplier(supplier);

        if (purchase.getPurchaseDetails() != null) {

            for (int i = 0; i < purchase.getPurchaseDetails().size(); i++) {

                PurchaseDetails detail = purchase.getPurchaseDetails().get(i);
                var dto = purchaseDto.getPurchaseDetails().get(i);

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

        purchase.setGrnNo(generateGrnNo(pharmacyId));
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

        if (savedPurchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : savedPurchase.getPurchaseDetails()) {

                ProductDetails product = detail.getProduct();
                BatchDetails batch = detail.getBatch();
                PackagingDetails packaging = batch.getPackagingDetails();

                Long purchaseQty = detail.getPurchaseQuantity() != null
                        ? detail.getPurchaseQuantity()
                        : 0L;

                Long purchaseUnitContains = (packaging != null
                        && packaging.getPurchaseUnitContains() != null)
                        ? packaging.getPurchaseUnitContains()
                        : 1L;

             // Stock in smallest units
                Long stockQty = purchaseQty * purchaseUnitContains;

                Inventory inventory = inventoryRepository
                        .findByProductAndPackagingAndBatch(product, packaging, batch)
                        .orElse(null);

                if (inventory == null) {

                    inventory = new Inventory();

                    inventory.setPharmacy(pharmacy);
                    inventory.setProduct(product);
                    inventory.setPackaging(packaging);
                    inventory.setBatch(batch);

                    inventory.setTotalStock(stockQty);

                    inventory.setCreatedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setCreatedAt(LocalDateTime.now());
                    inventory.setModifiedBy(null);
                    inventory.setModifiedAt(null);

                }

                else {

                    // Populate pharmacy if older inventory records don't have it
                    if (inventory.getPharmacy() == null) {
                        inventory.setPharmacy(pharmacy);
                    }

                    Long currentStock = inventory.getTotalStock() != null
                            ? inventory.getTotalStock()
                            : 0L;

                    inventory.setTotalStock(currentStock + stockQty);

                    inventory.setModifiedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setModifiedAt(LocalDateTime.now());
                }

                inventory = inventoryRepository.save(inventory);


                InventoryAudit audit = new InventoryAudit();

                audit.setInventory(inventory);
                audit.setPharmacy(pharmacy);
                audit.setPurchaseDetails(detail);
                audit.setStockMovement(StockMovement.IN);
                audit.setTransactionType(TransactionType.PURCHASE);
                audit.setChangeStock(stockQty);
                audit.setRemainingStock(inventory.getTotalStock());
                audit.setChangedBy(String.valueOf(persistentUser.getUserId()));
                audit.setChangedAt(LocalDateTime.now());

                inventoryAuditRepository.save(audit);
            }
        }

        return PurchaseMapper.toDto(savedPurchase);
    }


    @Override
    public List<PurchaseDto> getAllPurchases(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return purchaseRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(PurchaseMapper::toDto)
                .collect(Collectors.toList());
    }

    private String generateGrnNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "GRN-" + year + "-";

        List<String> latest = purchaseRepository.findLatestGrn(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestGrn = latest.get(0);

            String numberPart = latestGrn.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }
}