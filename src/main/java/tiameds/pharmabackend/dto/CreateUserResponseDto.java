package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateUserResponseDto {

    private UserDetailsDto user;
    private List<FeaturePermissionsDto> permissions;
}
