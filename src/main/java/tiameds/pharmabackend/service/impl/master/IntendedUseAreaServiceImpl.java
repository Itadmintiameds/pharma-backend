package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.IntendedUseAreaDto;
import tiameds.pharmabackend.entity.master.IntendedUseArea;
import tiameds.pharmabackend.repository.master.IntendedUseAreaRepository;
import tiameds.pharmabackend.service.master.IntendedUseAreaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class IntendedUseAreaServiceImpl implements IntendedUseAreaService {

    private final IntendedUseAreaRepository intendedUseAreaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IntendedUseAreaDto> getAllIntendedUseAreas() {
        return intendedUseAreaRepository
                .findAll(Sort.by("intendedUseAreaId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IntendedUseAreaDto getIntendedUseAreaById(Long intendedUseAreaId) {
        return toDto(findIntendedUseArea(intendedUseAreaId));
    }

    @Override
    public IntendedUseAreaDto createIntendedUseArea(IntendedUseAreaDto intendedUseAreaDto) {

        IntendedUseArea intendedUseArea = new IntendedUseArea();
        intendedUseArea.setIntendedUseAreaName(intendedUseAreaDto.getIntendedUseAreaName());
        intendedUseArea.setIsActive(
                intendedUseAreaDto.getIsActive() != null ? intendedUseAreaDto.getIsActive() : true);
        intendedUseArea.setCreatedAt(LocalDateTime.now());

        return toDto(intendedUseAreaRepository.save(intendedUseArea));
    }

    @Override
    public IntendedUseAreaDto updateIntendedUseArea(Long intendedUseAreaId,
                                                    IntendedUseAreaDto intendedUseAreaDto) {

        IntendedUseArea intendedUseArea = findIntendedUseArea(intendedUseAreaId);

        intendedUseArea.setIntendedUseAreaName(intendedUseAreaDto.getIntendedUseAreaName());
        if (intendedUseAreaDto.getIsActive() != null) {
            intendedUseArea.setIsActive(intendedUseAreaDto.getIsActive());
        }
        intendedUseArea.setModifiedAt(LocalDateTime.now());

        return toDto(intendedUseAreaRepository.save(intendedUseArea));
    }

    @Override
    public IntendedUseAreaDto updateIntendedUseAreaStatus(Long intendedUseAreaId, Boolean isActive) {

        IntendedUseArea intendedUseArea = findIntendedUseArea(intendedUseAreaId);

        intendedUseArea.setIsActive(isActive);
        intendedUseArea.setModifiedAt(LocalDateTime.now());

        return toDto(intendedUseAreaRepository.save(intendedUseArea));
    }

    private IntendedUseArea findIntendedUseArea(Long intendedUseAreaId) {
        return intendedUseAreaRepository.findById(intendedUseAreaId)
                .orElseThrow(() -> new RuntimeException(
                        "Intended use area not found with id: " + intendedUseAreaId));
    }

    private IntendedUseAreaDto toDto(IntendedUseArea intendedUseArea) {
        IntendedUseAreaDto dto = new IntendedUseAreaDto();
        dto.setIntendedUseAreaId(intendedUseArea.getIntendedUseAreaId());
        dto.setIntendedUseAreaName(intendedUseArea.getIntendedUseAreaName());
        dto.setIsActive(intendedUseArea.getIsActive());
        return dto;
    }
}
