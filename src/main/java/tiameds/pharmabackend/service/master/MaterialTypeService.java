package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.MaterialTypeDto;

import java.util.List;

public interface MaterialTypeService {

    List<MaterialTypeDto> getAllMaterialTypes();

    MaterialTypeDto getMaterialTypeById(Long materialTypeId);

    List<MaterialTypeDto> getMaterialTypesByCategoryId(Long productCategoryId);

    MaterialTypeDto createMaterialType(MaterialTypeDto materialTypeDto);

    MaterialTypeDto updateMaterialType(Long materialTypeId, MaterialTypeDto materialTypeDto);

    MaterialTypeDto updateMaterialTypeStatus(Long materialTypeId, Boolean isActive);
}
