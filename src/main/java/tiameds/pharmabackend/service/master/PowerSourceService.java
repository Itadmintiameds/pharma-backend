package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.PowerSourceDto;

import java.util.List;

public interface PowerSourceService {

    List<PowerSourceDto> getAllPowerSources();

    PowerSourceDto getPowerSourceById(Long powerSourceId);

    PowerSourceDto createPowerSource(PowerSourceDto powerSourceDto);

    PowerSourceDto updatePowerSource(Long powerSourceId, PowerSourceDto powerSourceDto);

    PowerSourceDto updatePowerSourceStatus(Long powerSourceId, Boolean isActive);
}
