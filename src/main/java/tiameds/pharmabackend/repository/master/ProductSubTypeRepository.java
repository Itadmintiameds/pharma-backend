package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.ProductSubType;

import java.util.List;

public interface ProductSubTypeRepository extends JpaRepository<ProductSubType, Long> {

    List<ProductSubType> findByProductType_ProductTypeId(Long productTypeId);
}
