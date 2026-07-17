package tiameds.pharmabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tiameds.pharmabackend.entity.PharmaRoles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDetailsDto {

    private Long userId;
    private List<PharmacyDetailsDto> pharmacies;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String userEmail;
    private String fullName;
    private String userPhone;
    private String employeeId;
    private LocalDate dob;
    private String gender;
    private String department;
    private String imageUrl;
    private PharmaRolesDto pharmaRolesDto;
    private String userStatus;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

}
