package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;

import java.util.List;

public interface NetQuantityUnitRepository extends JpaRepository<NetQuantityUnit, Long> {

    List<NetQuantityUnit> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
