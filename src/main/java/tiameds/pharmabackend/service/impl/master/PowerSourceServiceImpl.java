package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.PowerSourceDto;
import tiameds.pharmabackend.entity.master.PowerSource;
import tiameds.pharmabackend.repository.master.PowerSourceRepository;
import tiameds.pharmabackend.service.master.PowerSourceService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PowerSourceServiceImpl implements PowerSourceService {

    private final PowerSourceRepository powerSourceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PowerSourceDto> getAllPowerSources() {
        return powerSourceRepository
                .findAll(Sort.by("powerSourceId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PowerSourceDto getPowerSourceById(Long powerSourceId) {
        return toDto(findPowerSource(powerSourceId));
    }

    @Override
    public PowerSourceDto createPowerSource(PowerSourceDto powerSourceDto) {

        PowerSource powerSource = new PowerSource();
        powerSource.setPowerSourceName(powerSourceDto.getPowerSourceName());
        powerSource.setIsActive(powerSourceDto.getIsActive() != null ? powerSourceDto.getIsActive() : true);
        powerSource.setCreatedAt(LocalDateTime.now());

        return toDto(powerSourceRepository.save(powerSource));
    }

    @Override
    public PowerSourceDto updatePowerSource(Long powerSourceId, PowerSourceDto powerSourceDto) {

        PowerSource powerSource = findPowerSource(powerSourceId);

        powerSource.setPowerSourceName(powerSourceDto.getPowerSourceName());
        if (powerSourceDto.getIsActive() != null) {
            powerSource.setIsActive(powerSourceDto.getIsActive());
        }
        powerSource.setModifiedAt(LocalDateTime.now());

        return toDto(powerSourceRepository.save(powerSource));
    }

    @Override
    public PowerSourceDto updatePowerSourceStatus(Long powerSourceId, Boolean isActive) {

        PowerSource powerSource = findPowerSource(powerSourceId);

        powerSource.setIsActive(isActive);
        powerSource.setModifiedAt(LocalDateTime.now());

        return toDto(powerSourceRepository.save(powerSource));
    }

    private PowerSource findPowerSource(Long powerSourceId) {
        return powerSourceRepository.findById(powerSourceId)
                .orElseThrow(() -> new RuntimeException("Power source not found with id: " + powerSourceId));
    }

    private PowerSourceDto toDto(PowerSource powerSource) {
        PowerSourceDto dto = new PowerSourceDto();
        dto.setPowerSourceId(powerSource.getPowerSourceId());
        dto.setPowerSourceName(powerSource.getPowerSourceName());
        dto.setIsActive(powerSource.getIsActive());
        return dto;
    }
}
