package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
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

    // pharmacy-scoped variant, used when stock is issued out (billing)
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
}
