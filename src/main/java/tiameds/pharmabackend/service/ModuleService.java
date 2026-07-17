package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.ModuleDto;
import tiameds.pharmabackend.dto.ModuleSummaryDto;

import java.util.List;

public interface ModuleService {

    List<ModuleDto> getModulesWithFeatures();

    List<ModuleSummaryDto> getAllModules();

    ModuleDto getModuleWithFeatures(Long moduleId);
}
