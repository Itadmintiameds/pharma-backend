package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsRequestDto {

    private List<FeaturePermissionsDto> permissions;
}
