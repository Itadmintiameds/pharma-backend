package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.BatchDetails;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchDetailsRepository extends JpaRepository<BatchDetails, String> {

    // every batch belonging to the products of one pharmacy
    // OLD: traversed ProductDetails.pharmacy (ManyToOne), removed in favour of the
    // pharmacies (ManyToMany) collection.
    // List<BatchDetails> findByProduct_Pharmacy_PharmacyId(String pharmacyId);
    List<BatchDetails> findByProduct_Pharmacies_PharmacyId(String pharmacyId);

    // OLD: traversed ProductDetails.pharmacy (ManyToOne).
    // Optional<BatchDetails> findByBatchIdAndProduct_Pharmacy_PharmacyId(
    //         String batchId,
    //         String pharmacyId
    // );
    Optional<BatchDetails> findByBatchIdAndProduct_Pharmacies_PharmacyId(
            String batchId,
            String pharmacyId
    );

    // warehouse-scoped equivalents (Product <-> Warehouse ManyToMany)
    List<BatchDetails> findByProduct_Warehouses_WarehouseId(String warehouseId);

    Optional<BatchDetails> findByBatchIdAndProduct_Warehouses_WarehouseId(
            String batchId,
            String warehouseId
    );

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(batch_id, LENGTH(batch_id) - 4, 5) AS INTEGER))
        FROM pharma_batch_details
    """, nativeQuery = true)
    Integer findMaxBatchNumber();

    boolean existsByBatchNumberAndProduct_ProductIdAndPackagingDetails_PackagingId(
            String batchNumber,
            String productId,
            String packagingId
    );


}
