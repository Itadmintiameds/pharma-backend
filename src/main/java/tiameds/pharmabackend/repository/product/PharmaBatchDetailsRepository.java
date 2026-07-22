package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PharmaBatchDetails;

@Repository
public interface PharmaBatchDetailsRepository extends JpaRepository<PharmaBatchDetails, String> {

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(batch_id, LENGTH(batch_id) - 4, 5) AS INTEGER))
        FROM pharma_batch_details
    """, nativeQuery = true)
    Integer findMaxBatchNumber();
}
