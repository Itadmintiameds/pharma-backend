package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.ProductAttributeDrug;

@Repository
public interface ProductAttributeDrugRepository extends JpaRepository<ProductAttributeDrug, String> {
}
