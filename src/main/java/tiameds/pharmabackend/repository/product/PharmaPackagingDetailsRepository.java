package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PharmaPackagingDetails;

@Repository
public interface PharmaPackagingDetailsRepository extends JpaRepository<PharmaPackagingDetails, String> {
}
