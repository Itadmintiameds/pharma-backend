package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.PharmaProductAttributeDrug;

@Repository
public interface PharmaProductAttributeDrugRepository extends JpaRepository<PharmaProductAttributeDrug, String> {
}
