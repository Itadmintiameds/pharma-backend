package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.DeviceSubCategoryDto;

import java.util.List;

public interface DeviceSubCategoryService {

    List<DeviceSubCategoryDto> getAllDeviceSubCategories();

    DeviceSubCategoryDto getDeviceSubCategoryById(Long deviceSubCategoryId);

    List<DeviceSubCategoryDto> getDeviceSubCategoriesByDeviceCategoryId(Long deviceCategoryId);

    DeviceSubCategoryDto createDeviceSubCategory(DeviceSubCategoryDto deviceSubCategoryDto);

    DeviceSubCategoryDto updateDeviceSubCategory(Long deviceSubCategoryId, DeviceSubCategoryDto deviceSubCategoryDto);

    DeviceSubCategoryDto updateDeviceSubCategoryStatus(Long deviceSubCategoryId, Boolean isActive);
}
