package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.DeviceSpecificationUnitDto;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.repository.master.DeviceSpecificationUnitRepository;
import tiameds.pharmabackend.repository.master.DeviceSubCategoryRepository;
import tiameds.pharmabackend.service.master.DeviceSpecificationUnitService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceSpecificationUnitServiceImpl implements DeviceSpecificationUnitService {

    private final DeviceSpecificationUnitRepository deviceSpecificationUnitRepository;
    private final DeviceSubCategoryRepository deviceSubCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSpecificationUnitDto> getAllDeviceSpecificationUnits() {
        return deviceSpecificationUnitRepository
                .findAll(Sort.by("deviceSpecificationUnitId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceSpecificationUnitDto getDeviceSpecificationUnitById(Long deviceSpecificationUnitId) {
        return toDto(findDeviceSpecificationUnit(deviceSpecificationUnitId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSpecificationUnitDto> getDeviceSpecificationUnitsByDeviceSubCategoryId(Long deviceSubCategoryId) {

        if (!deviceSubCategoryRepository.existsById(deviceSubCategoryId)) {
            throw new RuntimeException("Device sub category not found with id: " + deviceSubCategoryId);
        }

        return deviceSpecificationUnitRepository
                .findByDeviceSubCategory_DeviceSubCategoryId(deviceSubCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceSpecificationUnitDto createDeviceSpecificationUnit(DeviceSpecificationUnitDto deviceSpecificationUnitDto) {

        DeviceSubCategory deviceSubCategory = findDeviceSubCategory(deviceSpecificationUnitDto.getDeviceSubCategoryId());

        DeviceSpecificationUnit deviceSpecificationUnit = new DeviceSpecificationUnit();
        deviceSpecificationUnit.setDeviceSpecificationUnitName(deviceSpecificationUnitDto.getDeviceSpecificationUnitName());
        deviceSpecificationUnit.setDeviceSubCategory(deviceSubCategory);
        deviceSpecificationUnit.setIsActive(
                deviceSpecificationUnitDto.getIsActive() != null ? deviceSpecificationUnitDto.getIsActive() : true);
        deviceSpecificationUnit.setCreatedAt(LocalDateTime.now());

        return toDto(deviceSpecificationUnitRepository.save(deviceSpecificationUnit));
    }

    @Override
    public DeviceSpecificationUnitDto updateDeviceSpecificationUnit(Long deviceSpecificationUnitId,
                                                                    DeviceSpecificationUnitDto deviceSpecificationUnitDto) {

        DeviceSpecificationUnit deviceSpecificationUnit = findDeviceSpecificationUnit(deviceSpecificationUnitId);

        deviceSpecificationUnit.setDeviceSpecificationUnitName(deviceSpecificationUnitDto.getDeviceSpecificationUnitName());

        if (deviceSpecificationUnitDto.getDeviceSubCategoryId() != null) {
            deviceSpecificationUnit.setDeviceSubCategory(findDeviceSubCategory(deviceSpecificationUnitDto.getDeviceSubCategoryId()));
        }

        if (deviceSpecificationUnitDto.getIsActive() != null) {
            deviceSpecificationUnit.setIsActive(deviceSpecificationUnitDto.getIsActive());
        }
        deviceSpecificationUnit.setUpdatedAt(LocalDateTime.now());

        return toDto(deviceSpecificationUnitRepository.save(deviceSpecificationUnit));
    }

    @Override
    public DeviceSpecificationUnitDto updateDeviceSpecificationUnitStatus(Long deviceSpecificationUnitId, Boolean isActive) {

        DeviceSpecificationUnit deviceSpecificationUnit = findDeviceSpecificationUnit(deviceSpecificationUnitId);

        deviceSpecificationUnit.setIsActive(isActive);
        deviceSpecificationUnit.setUpdatedAt(LocalDateTime.now());

        return toDto(deviceSpecificationUnitRepository.save(deviceSpecificationUnit));
    }

    private DeviceSpecificationUnit findDeviceSpecificationUnit(Long deviceSpecificationUnitId) {
        return deviceSpecificationUnitRepository.findById(deviceSpecificationUnitId)
                .orElseThrow(() -> new RuntimeException(
                        "Device specification unit not found with id: " + deviceSpecificationUnitId));
    }

    private DeviceSubCategory findDeviceSubCategory(Long deviceSubCategoryId) {

        if (deviceSubCategoryId == null) {
            throw new RuntimeException("Device sub category id is required");
        }

        return deviceSubCategoryRepository.findById(deviceSubCategoryId)
                .orElseThrow(() -> new RuntimeException("Device sub category not found with id: " + deviceSubCategoryId));
    }

    private DeviceSpecificationUnitDto toDto(DeviceSpecificationUnit deviceSpecificationUnit) {
        DeviceSpecificationUnitDto dto = new DeviceSpecificationUnitDto();
        dto.setDeviceSpecificationUnitId(deviceSpecificationUnit.getDeviceSpecificationUnitId());
        dto.setDeviceSpecificationUnitName(deviceSpecificationUnit.getDeviceSpecificationUnitName());

        if (deviceSpecificationUnit.getDeviceSubCategory() != null) {
            dto.setDeviceSubCategoryId(deviceSpecificationUnit.getDeviceSubCategory().getDeviceSubCategoryId());
            dto.setDeviceSubCategoryName(deviceSpecificationUnit.getDeviceSubCategory().getDeviceSubCategoryName());
        }

        dto.setIsActive(deviceSpecificationUnit.getIsActive());
        return dto;
    }
}
