package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.DistributionStatus;

import java.time.LocalDateTime;

/**
 * One entry of a distribution's status history (created / dispatched / received / rejected).
 */
@Data
public class WarehouseDistributionStatusResponse {

    private Long warehouseDistributionStatusId;
    private DistributionStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
}
