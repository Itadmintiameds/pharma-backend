package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDate;

// One return line of the Return Transactions table.
@Data
@JsonPropertyOrder({
        "returnDate", "referenceNumber", "returnType", "returnTypeLabel",
        "sourceName", "destinationName",
        "productName", "batchNo", "expiryDate",
        "returnReason", "returnedQuantity"})
public class StockReturnReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate returnDate;

    private String referenceNumber;

    // PHARMACY_TO_WAREHOUSE etc., and its label ("Pharmacy → Warehouse").
    private String returnType;
    private String returnTypeLabel;

    // Pharmacy / warehouse the stock left from.
    private String sourceName;

    // Warehouse or supplier the stock went to.
    private String destinationName;

    private String productName;
    private String batchNo;

    // Shown as "12/2027".
    @JsonFormat(pattern = "MM/yyyy")
    private LocalDate expiryDate;

    // DAMAGE / EXPIRY / OTHER.
    private String returnReason;

    private Long returnedQuantity;
}
