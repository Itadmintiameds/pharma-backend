package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.IntendedUseAreaDto;

import java.util.List;

public interface IntendedUseAreaService {

    List<IntendedUseAreaDto> getAllIntendedUseAreas();

    IntendedUseAreaDto getIntendedUseAreaById(Long intendedUseAreaId);

    IntendedUseAreaDto createIntendedUseArea(IntendedUseAreaDto intendedUseAreaDto);

    IntendedUseAreaDto updateIntendedUseArea(Long intendedUseAreaId, IntendedUseAreaDto intendedUseAreaDto);

    IntendedUseAreaDto updateIntendedUseAreaStatus(Long intendedUseAreaId, Boolean isActive);
}
