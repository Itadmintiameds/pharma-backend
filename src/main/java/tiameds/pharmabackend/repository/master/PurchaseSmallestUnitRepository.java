package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.PurchaseSmallestUnit;

import java.util.List;

public interface PurchaseSmallestUnitRepository extends JpaRepository<PurchaseSmallestUnit, Long> {

    List<PurchaseSmallestUnit> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
