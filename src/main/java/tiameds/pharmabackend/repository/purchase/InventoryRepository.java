package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository <Inventory, Long> {

    Optional<Inventory> findByProductAndPackagingAndBatch(
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch
    );
}
