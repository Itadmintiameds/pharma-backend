package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.HairTypeDto;

import java.util.List;

public interface HairTypeService {

    List<HairTypeDto> getAllHairTypes();

    HairTypeDto getHairTypeById(Long hairTypeId);

    HairTypeDto createHairType(HairTypeDto hairTypeDto);

    HairTypeDto updateHairType(Long hairTypeId, HairTypeDto hairTypeDto);

    HairTypeDto updateHairTypeStatus(Long hairTypeId, Boolean isActive);
}
