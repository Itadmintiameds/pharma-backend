package tiameds.pharmabackend.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.dto.FeatureDto;
import tiameds.pharmabackend.dto.ModuleDto;
import tiameds.pharmabackend.dto.ModuleSummaryDto;
import tiameds.pharmabackend.entity.PharmaFeature;
import tiameds.pharmabackend.entity.PharmaModule;
import tiameds.pharmabackend.repository.PharmaModuleRepository;
import tiameds.pharmabackend.service.ModuleService;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final PharmaModuleRepository pharmaModuleRepository;

    @Override
    public List<ModuleDto> getModulesWithFeatures() {

        return pharmaModuleRepository
                .findAllWithFeatures()
                .stream()
                .map(this::toModuleDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleSummaryDto> getAllModules() {

        return pharmaModuleRepository
                .findAll(Sort.by("moduleId"))
                .stream()
                .map(module -> {
                    ModuleSummaryDto dto = new ModuleSummaryDto();
                    dto.setModuleId(module.getModuleId());
                    dto.setModuleName(module.getModuleName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ModuleDto getModuleWithFeatures(Long moduleId) {

        PharmaModule module = pharmaModuleRepository
                .findById(moduleId)
                .orElseThrow(() ->
                        new RuntimeException("Module not found with id : " + moduleId));

        return toModuleDto(module);
    }

    private ModuleDto toModuleDto(PharmaModule module) {

        ModuleDto dto = new ModuleDto();
        dto.setModuleId(module.getModuleId());
        dto.setModuleName(module.getModuleName());
        dto.setFeatures(module.getFeatures()
                .stream()
                .sorted(Comparator.comparing(PharmaFeature::getFeatureId))
                .map(this::toFeatureDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private FeatureDto toFeatureDto(PharmaFeature feature) {

        FeatureDto dto = new FeatureDto();
        dto.setFeatureId(feature.getFeatureId());
        dto.setFeatureCode(feature.getFeatureCode());
        dto.setFeatureName(feature.getFeatureName());

        return dto;
    }
}
