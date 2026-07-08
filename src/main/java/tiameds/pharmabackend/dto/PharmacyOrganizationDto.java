package tiameds.pharmabackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PharmacyOrganizationDto {

    private Long organizationId;
    private String organizationName;
    private String organizationType;
    private String ownershipType;
    private String panNumber;
    private String gstNumber;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Boolean isRejected;
}
