package tiameds.pharmabackend.dto;

import lombok.Data;
import tiameds.pharmabackend.entity.PharmaRoles;

import java.time.LocalDateTime;

@Data
public class UserDetailsDto {

    private Long userId;
    private String pharmacyRegistrationId;
    private Long pharmacyId;
    private String userName;
    private String password;
    private String userEmail;
    private PharmaRolesDto pharmaRolesDto;
    private Boolean userStatus;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

}
