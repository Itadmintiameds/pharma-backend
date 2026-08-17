package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.DistributionStatus;
import tiameds.pharmabackend.enums.LocationType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarehouseDistributionResponse {

    private Long warehouseDistributionId;
    private String allocationMode;
    private String allocationNo;
    private LocalDateTime allocationDate;
    private String distributionType;
    private String reference;
    private String remarks;

    private LocationType sourceType;
    private String sourceId;
    private LocationType destinationType;
    private String destinationId;
    private String allocationRequestedBy;

    private DistributionStatus currentStatus;
    private List<WarehouseDistributionLineResponse> lines;

    private String createdBy;
    private LocalDateTime createdAt;
}
