package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.AgeGroupDto;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.repository.master.AgeGroupRepository;
import tiameds.pharmabackend.service.master.AgeGroupService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AgeGroupServiceImpl implements AgeGroupService {

    private final AgeGroupRepository ageGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AgeGroupDto> getAllAgeGroups() {
        return ageGroupRepository
                .findAll(Sort.by("ageGroupId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AgeGroupDto getAgeGroupById(Long ageGroupId) {
        return toDto(findAgeGroup(ageGroupId));
    }

    @Override
    public AgeGroupDto createAgeGroup(AgeGroupDto ageGroupDto) {

        AgeGroup ageGroup = new AgeGroup();
        ageGroup.setAgeGroupName(ageGroupDto.getAgeGroupName());
        ageGroup.setIsActive(ageGroupDto.getIsActive() != null ? ageGroupDto.getIsActive() : true);
        ageGroup.setCreatedAt(LocalDateTime.now());

        return toDto(ageGroupRepository.save(ageGroup));
    }

    @Override
    public AgeGroupDto updateAgeGroup(Long ageGroupId, AgeGroupDto ageGroupDto) {

        AgeGroup ageGroup = findAgeGroup(ageGroupId);

        ageGroup.setAgeGroupName(ageGroupDto.getAgeGroupName());
        if (ageGroupDto.getIsActive() != null) {
            ageGroup.setIsActive(ageGroupDto.getIsActive());
        }
        ageGroup.setModifiedAt(LocalDateTime.now());

        return toDto(ageGroupRepository.save(ageGroup));
    }

    @Override
    public AgeGroupDto updateAgeGroupStatus(Long ageGroupId, Boolean isActive) {

        AgeGroup ageGroup = findAgeGroup(ageGroupId);

        ageGroup.setIsActive(isActive);
        ageGroup.setModifiedAt(LocalDateTime.now());

        return toDto(ageGroupRepository.save(ageGroup));
    }

    private AgeGroup findAgeGroup(Long ageGroupId) {
        return ageGroupRepository.findById(ageGroupId)
                .orElseThrow(() -> new RuntimeException("Age group not found with id: " + ageGroupId));
    }

    private AgeGroupDto toDto(AgeGroup ageGroup) {
        AgeGroupDto dto = new AgeGroupDto();
        dto.setAgeGroupId(ageGroup.getAgeGroupId());
        dto.setAgeGroupName(ageGroup.getAgeGroupName());
        dto.setIsActive(ageGroup.getIsActive());
        return dto;
    }
}
