package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;

import java.util.List;

public interface DeviceSpecificationUnitRepository extends JpaRepository<DeviceSpecificationUnit, Long> {

    List<DeviceSpecificationUnit> findByDeviceSubCategory_DeviceSubCategoryId(Long deviceSubCategoryId);
}
