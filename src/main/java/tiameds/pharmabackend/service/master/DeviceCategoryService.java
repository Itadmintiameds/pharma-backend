package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.DeviceCategoryDto;

import java.util.List;

public interface DeviceCategoryService {

    List<DeviceCategoryDto> getAllDeviceCategories();

    DeviceCategoryDto getDeviceCategoryById(Long deviceCategoryId);

    List<DeviceCategoryDto> getDeviceCategoriesByCategoryId(Long productCategoryId);

    DeviceCategoryDto createDeviceCategory(DeviceCategoryDto deviceCategoryDto);

    DeviceCategoryDto updateDeviceCategory(Long deviceCategoryId, DeviceCategoryDto deviceCategoryDto);

    DeviceCategoryDto updateDeviceCategoryStatus(Long deviceCategoryId, Boolean isActive);
}
