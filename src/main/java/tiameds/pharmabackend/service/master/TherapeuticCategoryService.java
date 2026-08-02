package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.TherapeuticCategoryDto;

import java.util.List;

public interface TherapeuticCategoryService {

    List<TherapeuticCategoryDto> getAllTherapeuticCategories();

    TherapeuticCategoryDto getTherapeuticCategoryById(Long therapeuticCategoryId);

    TherapeuticCategoryDto createTherapeuticCategory(TherapeuticCategoryDto therapeuticCategoryDto);

    TherapeuticCategoryDto updateTherapeuticCategory(Long therapeuticCategoryId, TherapeuticCategoryDto therapeuticCategoryDto);

    TherapeuticCategoryDto updateTherapeuticCategoryStatus(Long therapeuticCategoryId, Boolean isActive);
}
