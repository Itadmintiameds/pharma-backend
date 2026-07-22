package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PharmaProductAttributeSupplements;

@Repository
public interface PharmaProductAttributeSupplementsRepository extends JpaRepository<PharmaProductAttributeSupplements, String> {
}
