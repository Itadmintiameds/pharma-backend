package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.ProductDetails;

@Repository
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, String> {

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(product_id, LENGTH(product_id) - 4, 5) AS INTEGER))
        FROM pharma_product_details
    """, nativeQuery = true)
    Integer findMaxProductNumber();
}
