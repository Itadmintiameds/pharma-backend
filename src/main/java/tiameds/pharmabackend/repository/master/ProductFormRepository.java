package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.ProductForm;

import java.util.List;

public interface ProductFormRepository extends JpaRepository<ProductForm, Long> {

    List<ProductForm> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
