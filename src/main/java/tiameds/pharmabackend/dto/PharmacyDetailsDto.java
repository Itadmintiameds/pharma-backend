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
    private String pharmacyDlno;
    private String pharmacyGstno;
    private String pharmacyPan;
    private String pharmacyBusinessRegistrationNo;
    private LocalDateTime pharmacyDlExpiryDate;
    private String pharmacyAddress;
    private String pharmacyLogo;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<UserDetailsDto> users;
}
