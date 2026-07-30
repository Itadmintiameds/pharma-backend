package tiameds.pharmabackend.service.impl.purchase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        PharmacyDetails pharmacy = pharmacyDetailsRepository.findById(purchaseDto.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Purchase purchase = PurchaseMapper.toEntity(purchaseDto);

        SupplierMaster supplier = supplierMasterRepository.findById(purchaseDto.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        purchase.setSupplier(supplier);

        /*
         * Map Product & Batch
         * Inventory table will maintain stock.
         * BatchDetails is used only for batch reference.
         */
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

                // Packaging is already mapped by PurchaseMapper
                // detail.setPackaging(...);
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

        /*
         * Inventory & Inventory Audit
         */
        if (savedPurchase.getPurchaseDetails() != null) {

            for (PurchaseDetails detail : savedPurchase.getPurchaseDetails()) {

                ProductDetails product = detail.getProduct();
                BatchDetails batch = detail.getBatch();
                PackagingDetails packaging = detail.getPackaging();

                Long purchaseQty = detail.getPurchaseQuantity() != null
                        ? detail.getPurchaseQuantity()
                        : 0L;

                Inventory inventory = inventoryRepository
                        .findByProductAndPackagingAndBatch(product, packaging, batch)
                        .orElse(null);

                /*
                 * Inventory does not exist
                 */
                if (inventory == null) {

                    inventory = new Inventory();

                    inventory.setPharmacy(pharmacy);
                    inventory.setProduct(product);
                    inventory.setPackaging(packaging);
                    inventory.setBatch(batch);

                    inventory.setTotalStock(purchaseQty);

                    inventory.setCreatedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setCreatedAt(LocalDateTime.now());
                    inventory.setModifiedBy(null);
                    inventory.setModifiedAt(null);

                }
                /*
                 * Inventory already exists
                 */
                else {

                    // Populate pharmacy if older inventory records don't have it
                    if (inventory.getPharmacy() == null) {
                        inventory.setPharmacy(pharmacy);
                    }

                    Long currentStock = inventory.getTotalStock() != null
                            ? inventory.getTotalStock()
                            : 0L;

                    inventory.setTotalStock(currentStock + purchaseQty);

                    inventory.setModifiedBy(String.valueOf(persistentUser.getUserId()));
                    inventory.setModifiedAt(LocalDateTime.now());
                }

                inventory = inventoryRepository.save(inventory);

                /*
                 * Inventory Audit
                 */
                InventoryAudit audit = new InventoryAudit();

                audit.setInventory(inventory);
                audit.setPharmacy(pharmacy);
                audit.setPurchaseDetails(detail);
                audit.setStockMovement(StockMovement.IN);
                audit.setTransactionType(TransactionType.PURCHASE);
                audit.setChangeStock(purchaseQty);
                audit.setRemainingStock(inventory.getTotalStock());
                audit.setChangedBy(String.valueOf(persistentUser.getUserId()));
                audit.setChangedAt(LocalDateTime.now());

                inventoryAuditRepository.save(audit);
            }
        }

        return PurchaseMapper.toDto(savedPurchase);
    }
}