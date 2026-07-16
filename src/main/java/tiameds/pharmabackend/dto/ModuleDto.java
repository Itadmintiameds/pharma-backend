package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModuleDto {

    private Long moduleId;
    private String moduleName;
    private List<FeatureDto> features;
}
