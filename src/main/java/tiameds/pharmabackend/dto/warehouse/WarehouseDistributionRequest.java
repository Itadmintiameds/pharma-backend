package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.LocationType;

import java.util.List;

// Payload to create a distribution / stock-transfer allocation.
@Data
public class WarehouseDistributionRequest {

    private String allocationMode;        // "Create Allocation By Myself" or "Against Stock Requirement"
    // Server-assigned on create (ignored if sent). Use GET /warehouse/distribution/next-allocation-no to preview.
    private String distributionType;      // "Warehouse Distribution" or "Pharmacy Transfer"
    private String reference;
    private String remarks;

    private LocationType sourceType;      // WAREHOUSE or PHARMACY
    private String sourceId;              // warehouse_id or pharmacy_id
    private LocationType destinationType; // WAREHOUSE or PHARMACY
    private String destinationId;

    private List<WarehouseDistributionLineRequest> lines;
}
