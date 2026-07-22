package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.TherapeuticCategoryDto;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;
import tiameds.pharmabackend.repository.master.TherapeuticCategoryRepository;
import tiameds.pharmabackend.service.master.TherapeuticCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TherapeuticCategoryServiceImpl implements TherapeuticCategoryService {

    private final TherapeuticCategoryRepository therapeuticCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TherapeuticCategoryDto> getAllTherapeuticCategories() {
        return therapeuticCategoryRepository
                .findAll(Sort.by("therapeuticCategoryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TherapeuticCategoryDto getTherapeuticCategoryById(Long therapeuticCategoryId) {
        return toDto(findTherapeuticCategory(therapeuticCategoryId));
    }

    @Override
    public TherapeuticCategoryDto createTherapeuticCategory(TherapeuticCategoryDto therapeuticCategoryDto) {

        TherapeuticCategory therapeuticCategory = new TherapeuticCategory();
        therapeuticCategory.setTherapeuticCategoryName(therapeuticCategoryDto.getTherapeuticCategoryName());
        therapeuticCategory.setIsActive(
                therapeuticCategoryDto.getIsActive() != null ? therapeuticCategoryDto.getIsActive() : true);
        therapeuticCategory.setCreatedAt(LocalDateTime.now());

        return toDto(therapeuticCategoryRepository.save(therapeuticCategory));
    }

    @Override
    public TherapeuticCategoryDto updateTherapeuticCategory(Long therapeuticCategoryId,
                                                            TherapeuticCategoryDto therapeuticCategoryDto) {

        TherapeuticCategory therapeuticCategory = findTherapeuticCategory(therapeuticCategoryId);

        therapeuticCategory.setTherapeuticCategoryName(therapeuticCategoryDto.getTherapeuticCategoryName());
        if (therapeuticCategoryDto.getIsActive() != null) {
            therapeuticCategory.setIsActive(therapeuticCategoryDto.getIsActive());
        }
        therapeuticCategory.setModifiedAt(LocalDateTime.now());

        return toDto(therapeuticCategoryRepository.save(therapeuticCategory));
    }

    @Override
    public TherapeuticCategoryDto updateTherapeuticCategoryStatus(Long therapeuticCategoryId, Boolean isActive) {

        TherapeuticCategory therapeuticCategory = findTherapeuticCategory(therapeuticCategoryId);

        therapeuticCategory.setIsActive(isActive);
        therapeuticCategory.setModifiedAt(LocalDateTime.now());

        return toDto(therapeuticCategoryRepository.save(therapeuticCategory));
    }

    private TherapeuticCategory findTherapeuticCategory(Long therapeuticCategoryId) {
        return therapeuticCategoryRepository.findById(therapeuticCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Therapeutic category not found with id: " + therapeuticCategoryId));
    }

    private TherapeuticCategoryDto toDto(TherapeuticCategory therapeuticCategory) {
        TherapeuticCategoryDto dto = new TherapeuticCategoryDto();
        dto.setTherapeuticCategoryId(therapeuticCategory.getTherapeuticCategoryId());
        dto.setTherapeuticCategoryName(therapeuticCategory.getTherapeuticCategoryName());
        dto.setIsActive(therapeuticCategory.getIsActive());
        return dto;
    }
}
