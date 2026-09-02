package tiameds.pharmabackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tiameds.pharmabackend.dto.warehouse.WarehouseSummaryDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDetailsDto {

    private String userId;
    @JsonIgnoreProperties("users")
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
    // OLD: single warehouse per user.
    // private String warehouseId;
    // private String warehouseName;
    private List<WarehouseSummaryDto> warehouses;
    private LocalDateTime lastLogin;
    private Boolean isRejected;
    private String userStatus;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    
    private List<UserFeaturePermissionDto> permissions;

    // Consent captured on the registration form. Write-only: an input to the
    // acceptance ledger, not a user attribute, so it is never echoed back.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean acceptedTerms;

}
