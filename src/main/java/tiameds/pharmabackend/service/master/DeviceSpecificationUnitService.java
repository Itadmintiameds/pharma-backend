package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.DeviceSpecificationUnitDto;

import java.util.List;

public interface DeviceSpecificationUnitService {

    List<DeviceSpecificationUnitDto> getAllDeviceSpecificationUnits();

    DeviceSpecificationUnitDto getDeviceSpecificationUnitById(Long deviceSpecificationUnitId);

    List<DeviceSpecificationUnitDto> getDeviceSpecificationUnitsByDeviceSubCategoryId(Long deviceSubCategoryId);

    DeviceSpecificationUnitDto createDeviceSpecificationUnit(DeviceSpecificationUnitDto deviceSpecificationUnitDto);

    DeviceSpecificationUnitDto updateDeviceSpecificationUnit(Long deviceSpecificationUnitId, DeviceSpecificationUnitDto deviceSpecificationUnitDto);

    DeviceSpecificationUnitDto updateDeviceSpecificationUnitStatus(Long deviceSpecificationUnitId, Boolean isActive);
}
