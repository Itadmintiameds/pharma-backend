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
    private Boolean centralizedInventory;

    // Sent from the frontend only for flow 3 (centralizedInventory = true):
    // the central warehouse to be created along with the organization.
    // private WarehouseDto warehouse;

    // Sent from the frontend only for flow 3 (centralizedInventory = true):
    // one or more warehouses to be created along with the organization.
    // A single-element list keeps the old single-warehouse behavior.
    private List<WarehouseDto> warehouses;
}
