package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

// One movement row of the Stock Movement Details table. source / destination
// are null where the movement has no such side (shown as "---").
@Data
@JsonPropertyOrder({
        "transactionDate", "movementType", "movementTypeLabel", "referenceNo",
        "locationType", "locationName", "source", "destination", "stockIn", "stockOut", "balanceQuantity"})
public class StockMovementReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy hh:mm a", locale = "en")
    private LocalDateTime transactionDate;

    // Code (ALLOCATION_IN) for filtering, label (Allocation In) for display.
    private String movementType;
    private String movementTypeLabel;

    private String referenceNo;

    // PHARMACY or WAREHOUSE whose stock moved.
    private String locationType;
    private String locationName;

    private String source;
    private String destination;

    // Both stay 0 on a Damage Capture row: damaged units never enter stock.
    private Long stockIn;
    private Long stockOut;

    // Opening stock plus every movement up to and including this one.
    private Long balanceQuantity;
}
