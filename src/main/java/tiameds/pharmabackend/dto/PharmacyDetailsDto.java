package tiameds.pharmabackend.dto;

import lombok.Data;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PharmacyDetailsDto {

    private String pharmacyId;
    private String pharmacyRegistrationId;
    private String pharmacyName;
    private String pharmacyType;
    private String pharmacyEmail;
    private Long pharmacyPhone;
    private String panNumber;
    private String gstNumber;
    private String pharmacyBranch;
    private String pharmacyBuildingNo;
    private String pharmacyStreet;
    private String pharmacyCity;
    private String pharmacyTaluka;
    private String pharmacyDistricts;
    private Long pharmacyPincode;
    private String pharmacyLandmark;
    private String pharmacyState;
    private String pharmacyLogo;
    private String userId;
    private String warehouseId;
    private String warehouseName;
    private PharmacyOrganizationDto pharmacyOrganization;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<UserDetailsDto> users;
    private List<PharmaDocumentsDto> documents;



}
