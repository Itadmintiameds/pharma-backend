package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.MaterialType;

import java.util.List;

public interface MaterialTypeRepository extends JpaRepository<MaterialType, Long> {

    List<MaterialType> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
