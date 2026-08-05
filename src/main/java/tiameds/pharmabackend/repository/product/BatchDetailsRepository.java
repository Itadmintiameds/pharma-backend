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
    List<BatchDetails> findByProduct_Pharmacy_PharmacyId(String pharmacyId);

    Optional<BatchDetails> findByBatchIdAndProduct_Pharmacy_PharmacyId(
            String batchId,
            String pharmacyId
    );

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(batch_id, LENGTH(batch_id) - 4, 5) AS INTEGER))
        FROM pharma_batch_details
    """, nativeQuery = true)
    Integer findMaxBatchNumber();
}
