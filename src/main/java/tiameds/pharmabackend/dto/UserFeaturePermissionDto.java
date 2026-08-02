package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class UserFeaturePermissionDto {

    private Long id;
    private Long featureId;
    private String featureName;
    private String featureCode;
    
    private Long permissionId;
    private String permissionName;
    
    private Long moduleId;
    private String moduleName;

}
