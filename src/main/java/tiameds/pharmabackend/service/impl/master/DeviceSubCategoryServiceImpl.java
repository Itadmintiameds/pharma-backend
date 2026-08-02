package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.DeviceSubCategoryDto;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.repository.master.DeviceCategoryRepository;
import tiameds.pharmabackend.repository.master.DeviceSubCategoryRepository;
import tiameds.pharmabackend.service.master.DeviceSubCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceSubCategoryServiceImpl implements DeviceSubCategoryService {

    private final DeviceSubCategoryRepository deviceSubCategoryRepository;
    private final DeviceCategoryRepository deviceCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSubCategoryDto> getAllDeviceSubCategories() {
        return deviceSubCategoryRepository
                .findAll(Sort.by("deviceSubCategoryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceSubCategoryDto getDeviceSubCategoryById(Long deviceSubCategoryId) {
        return toDto(findDeviceSubCategory(deviceSubCategoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSubCategoryDto> getDeviceSubCategoriesByDeviceCategoryId(Long deviceCategoryId) {

        if (!deviceCategoryRepository.existsById(deviceCategoryId)) {
            throw new RuntimeException("Device category not found with id: " + deviceCategoryId);
        }

        return deviceSubCategoryRepository
                .findByDeviceCategory_DeviceCategoryId(deviceCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceSubCategoryDto createDeviceSubCategory(DeviceSubCategoryDto deviceSubCategoryDto) {

        DeviceCategory deviceCategory = findDeviceCategory(deviceSubCategoryDto.getDeviceCategoryId());

        DeviceSubCategory deviceSubCategory = new DeviceSubCategory();
        deviceSubCategory.setDeviceSubCategoryName(deviceSubCategoryDto.getDeviceSubCategoryName());
        deviceSubCategory.setDeviceCategory(deviceCategory);
        deviceSubCategory.setIsActive(
                deviceSubCategoryDto.getIsActive() != null ? deviceSubCategoryDto.getIsActive() : true);
        deviceSubCategory.setCreatedAt(LocalDateTime.now());

        return toDto(deviceSubCategoryRepository.save(deviceSubCategory));
    }

    @Override
    public DeviceSubCategoryDto updateDeviceSubCategory(Long deviceSubCategoryId,
                                                        DeviceSubCategoryDto deviceSubCategoryDto) {

        DeviceSubCategory deviceSubCategory = findDeviceSubCategory(deviceSubCategoryId);

        deviceSubCategory.setDeviceSubCategoryName(deviceSubCategoryDto.getDeviceSubCategoryName());

        if (deviceSubCategoryDto.getDeviceCategoryId() != null) {
            deviceSubCategory.setDeviceCategory(findDeviceCategory(deviceSubCategoryDto.getDeviceCategoryId()));
        }

        if (deviceSubCategoryDto.getIsActive() != null) {
            deviceSubCategory.setIsActive(deviceSubCategoryDto.getIsActive());
        }
        deviceSubCategory.setModifiedAt(LocalDateTime.now());

        return toDto(deviceSubCategoryRepository.save(deviceSubCategory));
    }

    @Override
    public DeviceSubCategoryDto updateDeviceSubCategoryStatus(Long deviceSubCategoryId, Boolean isActive) {

        DeviceSubCategory deviceSubCategory = findDeviceSubCategory(deviceSubCategoryId);

        deviceSubCategory.setIsActive(isActive);
        deviceSubCategory.setModifiedAt(LocalDateTime.now());

        return toDto(deviceSubCategoryRepository.save(deviceSubCategory));
    }

    private DeviceSubCategory findDeviceSubCategory(Long deviceSubCategoryId) {
        return deviceSubCategoryRepository.findById(deviceSubCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Device sub category not found with id: " + deviceSubCategoryId));
    }

    private DeviceCategory findDeviceCategory(Long deviceCategoryId) {

        if (deviceCategoryId == null) {
            throw new RuntimeException("Device category id is required");
        }

        return deviceCategoryRepository.findById(deviceCategoryId)
                .orElseThrow(() -> new RuntimeException("Device category not found with id: " + deviceCategoryId));
    }

    private DeviceSubCategoryDto toDto(DeviceSubCategory deviceSubCategory) {
        DeviceSubCategoryDto dto = new DeviceSubCategoryDto();
        dto.setDeviceSubCategoryId(deviceSubCategory.getDeviceSubCategoryId());
        dto.setDeviceSubCategoryName(deviceSubCategory.getDeviceSubCategoryName());

        if (deviceSubCategory.getDeviceCategory() != null) {
            dto.setDeviceCategoryId(deviceSubCategory.getDeviceCategory().getDeviceCategoryId());
            dto.setDeviceCategoryName(deviceSubCategory.getDeviceCategory().getDeviceCategoryName());
        }

        dto.setIsActive(deviceSubCategory.getIsActive());
        return dto;
    }
}
