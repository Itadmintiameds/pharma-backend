package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeaturePermissionsDto {

    private Long featureId;
    private List<Long> permissionIds;
}
