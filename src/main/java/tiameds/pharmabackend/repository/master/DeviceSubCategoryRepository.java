package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;

import java.util.List;

public interface DeviceSubCategoryRepository extends JpaRepository<DeviceSubCategory, Long> {

    List<DeviceSubCategory> findByDeviceCategory_DeviceCategoryId(Long deviceCategoryId);
}
