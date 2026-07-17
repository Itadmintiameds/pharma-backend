package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CurrentUserPermissionsDto {

    private Long userId;
    private String roleName;
    private List<String> permissions;
}
