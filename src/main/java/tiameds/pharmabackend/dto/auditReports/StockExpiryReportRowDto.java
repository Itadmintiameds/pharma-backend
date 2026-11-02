package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDate;

// One location + product + batch row of the Expiry Details table.
@Data
@JsonPropertyOrder({
        "locationType", "locationName", "productName", "batchNo", "supplierName",
        "expiryDate", "monthsToExpiry", "expiryStatus", "quantity", "damagedQuantity"})
public class StockExpiryReportRowDto {

    // PHARMACY or WAREHOUSE.
    private String locationType;
    private String locationName;

    private String productName;
    private String batchNo;
    private String supplierName;

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate expiryDate;

    // Days left until expiry (negative once expired); the UI shows "(90 days)".
    private Long monthsToExpiry;

    // EXPIRED, EXPIRING_THIS_MONTH, EXPIRING_SOON or VALID.
    private String expiryStatus;

    // Units on hand, and damaged units received at the location that have not
    // been sent back (these never enter inventory).
    private Long quantity;
    private Long damagedQuantity;
}
