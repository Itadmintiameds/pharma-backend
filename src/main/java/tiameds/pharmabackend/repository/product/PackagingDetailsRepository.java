package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PackagingDetails;

@Repository
public interface PackagingDetailsRepository extends JpaRepository<PackagingDetails, String> {

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(packaging_id, LENGTH(packaging_id) - 4, 5) AS INTEGER))
        FROM pharma_packaging_details
    """, nativeQuery = true)
    Integer findMaxPackagingNumber();
}
