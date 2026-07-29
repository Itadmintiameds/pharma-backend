package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.DeviceCategory;

import java.util.List;

public interface DeviceCategoryRepository extends JpaRepository<DeviceCategory, Long> {

    List<DeviceCategory> findByProductCategory_ProductCategoryId(Long productCategoryId);
}
