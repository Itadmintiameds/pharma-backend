package tiameds.pharmabackend.repository.purchase;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository <Inventory, Long> {

    Optional<Inventory> findByProductAndPackagingAndBatch(
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch
    );

    // Pharmacy-scoped variant, used when stock is issued out (billing).
    // PESSIMISTIC_WRITE issues SELECT ... FOR UPDATE and holds the row until the
    // transaction commits, so two concurrent bills cannot both read the same
    // stock level, both pass the sufficiency check, and drive the stock negative.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
            String pharmacyId,
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch
    );

    // all stock rows for a pharmacy (used for the products stock summary)
    List<Inventory> findByPharmacy_PharmacyId(String pharmacyId);

    // all stock rows for a single product (used for the product details view)
    List<Inventory> findByProduct_ProductId(String productId);

    // stock rows of one batch within a pharmacy (used for the batch lookup)
    List<Inventory> findByPharmacy_PharmacyIdAndBatch_BatchId(String pharmacyId, String batchId);
}
