package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.ProductType;

import java.util.List;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    List<ProductType> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
