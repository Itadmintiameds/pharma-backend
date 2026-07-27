package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.NetQuantityUnitDto;

import java.util.List;

public interface NetQuantityUnitService {

    List<NetQuantityUnitDto> getAllNetQuantityUnits();

    NetQuantityUnitDto getNetQuantityUnitById(Long netQuantityUnitId);

    List<NetQuantityUnitDto> getNetQuantityUnitsByCategoryId(Long productCategoryId);

    NetQuantityUnitDto createNetQuantityUnit(NetQuantityUnitDto netQuantityUnitDto);

    NetQuantityUnitDto updateNetQuantityUnit(Long netQuantityUnitId, NetQuantityUnitDto netQuantityUnitDto);

    NetQuantityUnitDto updateNetQuantityUnitStatus(Long netQuantityUnitId, Boolean isActive);
}
