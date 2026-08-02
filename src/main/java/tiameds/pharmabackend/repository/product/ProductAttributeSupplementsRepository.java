package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.ProductAttributeSupplements;

@Repository
public interface ProductAttributeSupplementsRepository extends JpaRepository<ProductAttributeSupplements, String> {
}
