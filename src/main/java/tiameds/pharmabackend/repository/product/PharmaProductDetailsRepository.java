package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PharmaProductDetails;

@Repository
public interface PharmaProductDetailsRepository extends JpaRepository<PharmaProductDetails, String> {
}
