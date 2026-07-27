package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.ProductForm;

public interface ProductFormRepository extends JpaRepository<ProductForm, Long> {
}
