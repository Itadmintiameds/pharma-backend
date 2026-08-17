package tiameds.pharmabackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequestDto {

    private UserDetailsDto user;
    private List<String> pharmacyIds;
    // OLD: single warehouse per user.
    // private String warehouseId;
    private List<String> warehouseIds;
    private List<FeaturePermissionsDto> permissions;
}
