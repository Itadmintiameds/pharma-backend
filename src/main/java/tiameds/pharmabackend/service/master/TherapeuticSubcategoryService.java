package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.TherapeuticSubcategoryDto;

import java.util.List;

public interface TherapeuticSubcategoryService {

    List<TherapeuticSubcategoryDto> getAllTherapeuticSubcategories();

    TherapeuticSubcategoryDto getTherapeuticSubcategoryById(Long therapeuticSubcategoryId);

    List<TherapeuticSubcategoryDto> getSubcategoriesByCategoryId(Long therapeuticCategoryId);

    TherapeuticSubcategoryDto createTherapeuticSubcategory(TherapeuticSubcategoryDto therapeuticSubcategoryDto);

    TherapeuticSubcategoryDto updateTherapeuticSubcategory(Long therapeuticSubcategoryId, TherapeuticSubcategoryDto therapeuticSubcategoryDto);

    TherapeuticSubcategoryDto updateTherapeuticSubcategoryStatus(Long therapeuticSubcategoryId, Boolean isActive);
}
