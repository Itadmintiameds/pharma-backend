package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequestDto {

    private UserDetailsDto user;
    private List<String> pharmacyIds;
    private List<FeaturePermissionsDto> permissions;
}
