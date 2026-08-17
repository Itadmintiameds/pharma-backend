package tiameds.pharmabackend.repository.warehouse;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventory;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {

    // Warehouse-scoped stock row for one batch. PESSIMISTIC_WRITE issues
    // SELECT ... FOR UPDATE and holds the row until commit, so two concurrent
    // transfers cannot both read the same stock level, both pass the sufficiency
    // check, and drive the stock negative. (Mirrors InventoryRepository.)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WarehouseInventory> findByWarehouse_WarehouseIdAndProductAndPackagingAndBatch(
            String warehouseId,
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch
    );

    // all stock rows for a warehouse (stock summary)
    List<WarehouseInventory> findByWarehouse_WarehouseId(String warehouseId);

    // warehouse stock rows for one product (per-batch stock in product details)
    List<WarehouseInventory> findByWarehouse_WarehouseIdAndProduct_ProductId(
            String warehouseId, String productId);

    // warehouse stock rows for one batch (single-batch stock view)
    List<WarehouseInventory> findByWarehouse_WarehouseIdAndBatch_BatchId(
            String warehouseId, String batchId);
}
