package tiameds.pharmabackend.dto;

import lombok.Data;
import tiameds.pharmabackend.entity.PharmaRoles;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDetailsDto {

    private Long userId;
    private List<PharmacyDetailsDto> pharmacies;
    private String password;
    private String userEmail;
    private PharmaRolesDto pharmaRolesDto;
    private String userStatus;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

}
