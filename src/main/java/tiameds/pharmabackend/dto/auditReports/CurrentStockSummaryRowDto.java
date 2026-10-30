package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDate;

// One location + product + batch row of the Stock Details table.
// closingStock = damagedStock + expiredStock + availableStock.
@Data
@JsonPropertyOrder({
        "locationType", "locationName", "productName", "batchNo", "expiryDate",
        "closingStock", "damagedStock", "expiredStock", "availableStock"})
public class CurrentStockSummaryRowDto {

    // PHARMACY or WAREHOUSE.
    private String locationType;
    private String locationName;

    private String productName;
    private String batchNo;

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate expiryDate;

    private Long closingStock;
    private Long damagedStock;
    private Long expiredStock;
    private Long availableStock;
}
