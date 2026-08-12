package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarehouseDto {

    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private String warehouseAddress;
    private String contactPersonName;
    private String mobileNumber;
    private Boolean isActive;
    private Long organizationId;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

    // Read-only: product ids currently mapped to this warehouse
    private List<String> productIds;
}
