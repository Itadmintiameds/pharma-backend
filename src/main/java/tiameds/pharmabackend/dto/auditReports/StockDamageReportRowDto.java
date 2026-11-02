package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDate;

// One return line or adjustment of the Damage Transactions table.
@Data
@JsonPropertyOrder({
        "transactionDate", "productName", "batchNo", "expiryDate",
        "transactionType", "transactionTypeLabel", "referenceNumber",
        "sourceName", "destinationName",
        "returnedQuantity", "adjustedQuantity", "totalDamagedQuantity"})
public class StockDamageReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate transactionDate;

    private String productName;
    private String batchNo;

    // Shown as "12/2027".
    @JsonFormat(pattern = "MM/yyyy")
    private LocalDate expiryDate;

    // STOCK_RETURN or STOCK_ADJUSTMENT, and its label for display.
    private String transactionType;
    private String transactionTypeLabel;

    private String referenceNumber;

    // Pharmacy / warehouse the stock left from.
    private String sourceName;

    // Warehouse or supplier; null for an adjustment (UI shows "---").
    private String destinationName;

    private Long returnedQuantity;
    private Long adjustedQuantity;
    private Long totalDamagedQuantity;
}
