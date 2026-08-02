package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.DeviceCategoryDto;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.repository.master.DeviceCategoryRepository;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.service.master.DeviceCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceCategoryServiceImpl implements DeviceCategoryService {

    private final DeviceCategoryRepository deviceCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceCategoryDto> getAllDeviceCategories() {
        return deviceCategoryRepository
                .findAll(Sort.by("deviceCategoryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceCategoryDto getDeviceCategoryById(Long deviceCategoryId) {
        return toDto(findDeviceCategory(deviceCategoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceCategoryDto> getDeviceCategoriesByCategoryId(Long productCategoryId) {

        if (!productCategoryRepository.existsById(productCategoryId)) {
            throw new RuntimeException("Product category not found with id: " + productCategoryId);
        }

        return deviceCategoryRepository
                .findByProductCategory_ProductCategoryId(productCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceCategoryDto createDeviceCategory(DeviceCategoryDto deviceCategoryDto) {

        DeviceCategory deviceCategory = new DeviceCategory();
        deviceCategory.setDeviceCategoryName(deviceCategoryDto.getDeviceCategoryName());
        if (deviceCategoryDto.getProductCategoryId() != null) {
            deviceCategory.setProductCategory(findProductCategory(deviceCategoryDto.getProductCategoryId()));
        }
        deviceCategory.setIsActive(deviceCategoryDto.getIsActive() != null ? deviceCategoryDto.getIsActive() : true);
        deviceCategory.setCreatedAt(LocalDateTime.now());

        return toDto(deviceCategoryRepository.save(deviceCategory));
    }

    @Override
    public DeviceCategoryDto updateDeviceCategory(Long deviceCategoryId, DeviceCategoryDto deviceCategoryDto) {

        DeviceCategory deviceCategory = findDeviceCategory(deviceCategoryId);

        deviceCategory.setDeviceCategoryName(deviceCategoryDto.getDeviceCategoryName());
        if (deviceCategoryDto.getProductCategoryId() != null) {
            deviceCategory.setProductCategory(findProductCategory(deviceCategoryDto.getProductCategoryId()));
        }
        if (deviceCategoryDto.getIsActive() != null) {
            deviceCategory.setIsActive(deviceCategoryDto.getIsActive());
        }
        deviceCategory.setModifiedAt(LocalDateTime.now());

        return toDto(deviceCategoryRepository.save(deviceCategory));
    }

    @Override
    public DeviceCategoryDto updateDeviceCategoryStatus(Long deviceCategoryId, Boolean isActive) {

        DeviceCategory deviceCategory = findDeviceCategory(deviceCategoryId);

        deviceCategory.setIsActive(isActive);
        deviceCategory.setModifiedAt(LocalDateTime.now());

        return toDto(deviceCategoryRepository.save(deviceCategory));
    }

    private DeviceCategory findDeviceCategory(Long deviceCategoryId) {
        return deviceCategoryRepository.findById(deviceCategoryId)
                .orElseThrow(() -> new RuntimeException("Device category not found with id: " + deviceCategoryId));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + productCategoryId));
    }

    private DeviceCategoryDto toDto(DeviceCategory deviceCategory) {
        DeviceCategoryDto dto = new DeviceCategoryDto();
        dto.setDeviceCategoryId(deviceCategory.getDeviceCategoryId());
        dto.setDeviceCategoryName(deviceCategory.getDeviceCategoryName());

        if (deviceCategory.getProductCategory() != null) {
            dto.setProductCategoryId(deviceCategory.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(deviceCategory.getProductCategory().getProductCategoryName());
        }

        dto.setIsActive(deviceCategory.getIsActive());
        return dto;
    }
}
