package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeatureDto {

    private Long featureId;
    private String featureCode;
    private String featureName;
    private List<PermissionDto> permissions;
}
