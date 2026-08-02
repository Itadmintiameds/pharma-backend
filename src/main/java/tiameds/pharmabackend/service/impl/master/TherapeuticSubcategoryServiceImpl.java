package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.TherapeuticSubcategoryDto;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;
import tiameds.pharmabackend.repository.master.TherapeuticCategoryRepository;
import tiameds.pharmabackend.repository.master.TherapeuticSubcategoryRepository;
import tiameds.pharmabackend.service.master.TherapeuticSubcategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TherapeuticSubcategoryServiceImpl implements TherapeuticSubcategoryService {

    private final TherapeuticSubcategoryRepository therapeuticSubcategoryRepository;
    private final TherapeuticCategoryRepository therapeuticCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TherapeuticSubcategoryDto> getAllTherapeuticSubcategories() {
        return therapeuticSubcategoryRepository
                .findAll(Sort.by("therapeuticSubcategoryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TherapeuticSubcategoryDto getTherapeuticSubcategoryById(Long therapeuticSubcategoryId) {
        return toDto(findTherapeuticSubcategory(therapeuticSubcategoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TherapeuticSubcategoryDto> getSubcategoriesByCategoryId(Long therapeuticCategoryId) {

        if (!therapeuticCategoryRepository.existsById(therapeuticCategoryId)) {
            throw new RuntimeException("Therapeutic category not found with id: " + therapeuticCategoryId);
        }

        return therapeuticSubcategoryRepository
                .findByTherapeuticCategory_TherapeuticCategoryId(therapeuticCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TherapeuticSubcategoryDto createTherapeuticSubcategory(TherapeuticSubcategoryDto therapeuticSubcategoryDto) {

        TherapeuticCategory therapeuticCategory =
                findTherapeuticCategory(therapeuticSubcategoryDto.getTherapeuticCategoryId());

        TherapeuticSubcategory therapeuticSubcategory = new TherapeuticSubcategory();
        therapeuticSubcategory.setTherapeuticSubcategoryName(
                therapeuticSubcategoryDto.getTherapeuticSubcategoryName());
        therapeuticSubcategory.setTherapeuticCategory(therapeuticCategory);
        therapeuticSubcategory.setIsActive(
                therapeuticSubcategoryDto.getIsActive() != null ? therapeuticSubcategoryDto.getIsActive() : true);
        therapeuticSubcategory.setCreatedAt(LocalDateTime.now());

        return toDto(therapeuticSubcategoryRepository.save(therapeuticSubcategory));
    }

    @Override
    public TherapeuticSubcategoryDto updateTherapeuticSubcategory(Long therapeuticSubcategoryId,
                                                                  TherapeuticSubcategoryDto therapeuticSubcategoryDto) {

        TherapeuticSubcategory therapeuticSubcategory = findTherapeuticSubcategory(therapeuticSubcategoryId);

        therapeuticSubcategory.setTherapeuticSubcategoryName(
                therapeuticSubcategoryDto.getTherapeuticSubcategoryName());

        if (therapeuticSubcategoryDto.getTherapeuticCategoryId() != null) {
            therapeuticSubcategory.setTherapeuticCategory(
                    findTherapeuticCategory(therapeuticSubcategoryDto.getTherapeuticCategoryId()));
        }

        if (therapeuticSubcategoryDto.getIsActive() != null) {
            therapeuticSubcategory.setIsActive(therapeuticSubcategoryDto.getIsActive());
        }
        therapeuticSubcategory.setModifiedAt(LocalDateTime.now());

        return toDto(therapeuticSubcategoryRepository.save(therapeuticSubcategory));
    }

    @Override
    public TherapeuticSubcategoryDto updateTherapeuticSubcategoryStatus(Long therapeuticSubcategoryId,
                                                                        Boolean isActive) {

        TherapeuticSubcategory therapeuticSubcategory = findTherapeuticSubcategory(therapeuticSubcategoryId);

        therapeuticSubcategory.setIsActive(isActive);
        therapeuticSubcategory.setModifiedAt(LocalDateTime.now());

        return toDto(therapeuticSubcategoryRepository.save(therapeuticSubcategory));
    }

    private TherapeuticSubcategory findTherapeuticSubcategory(Long therapeuticSubcategoryId) {
        return therapeuticSubcategoryRepository.findById(therapeuticSubcategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Therapeutic subcategory not found with id: " + therapeuticSubcategoryId));
    }

    private TherapeuticCategory findTherapeuticCategory(Long therapeuticCategoryId) {

        if (therapeuticCategoryId == null) {
            throw new RuntimeException("Therapeutic category id is required");
        }

        return therapeuticCategoryRepository.findById(therapeuticCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Therapeutic category not found with id: " + therapeuticCategoryId));
    }

    private TherapeuticSubcategoryDto toDto(TherapeuticSubcategory therapeuticSubcategory) {
        TherapeuticSubcategoryDto dto = new TherapeuticSubcategoryDto();
        dto.setTherapeuticSubcategoryId(therapeuticSubcategory.getTherapeuticSubcategoryId());
        dto.setTherapeuticSubcategoryName(therapeuticSubcategory.getTherapeuticSubcategoryName());

        if (therapeuticSubcategory.getTherapeuticCategory() != null) {
            dto.setTherapeuticCategoryId(
                    therapeuticSubcategory.getTherapeuticCategory().getTherapeuticCategoryId());
            dto.setTherapeuticCategoryName(
                    therapeuticSubcategory.getTherapeuticCategory().getTherapeuticCategoryName());
        }

        dto.setIsActive(therapeuticSubcategory.getIsActive());
        return dto;
    }
}
