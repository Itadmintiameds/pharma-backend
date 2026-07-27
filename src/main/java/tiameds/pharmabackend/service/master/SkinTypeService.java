package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.SkinTypeDto;

import java.util.List;

public interface SkinTypeService {

    List<SkinTypeDto> getAllSkinTypes();

    SkinTypeDto getSkinTypeById(Long skinTypeId);

    SkinTypeDto createSkinType(SkinTypeDto skinTypeDto);

    SkinTypeDto updateSkinType(Long skinTypeId, SkinTypeDto skinTypeDto);

    SkinTypeDto updateSkinTypeStatus(Long skinTypeId, Boolean isActive);
}
