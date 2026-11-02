package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDate;

// One adjustment of the Adjustment Transactions table.
@Data
@JsonPropertyOrder({
        "adjustmentDate", "pharmacyName", "referenceNumber",
        "productName", "batchNo", "expiryDate",
        "adjustmentType", "adjustmentTypeLabel", "adjustmentReason",
        "stockBefore", "adjustmentIn", "adjustmentOut", "netAdjustment", "stockAfter",
        "adjustedByName"})
public class StockAdjustmentReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate adjustmentDate;

    private String pharmacyName;
    private String referenceNumber;
    private String productName;
    private String batchNo;

    // Shown as "12/2027".
    @JsonFormat(pattern = "MM/yyyy")
    private LocalDate expiryDate;

    // INCREASE or DECREASE, and its label ("Stock Increase") for display.
    private String adjustmentType;
    private String adjustmentTypeLabel;

    private String adjustmentReason;

    // stockAfter is the counted stock the adjustment set; stockBefore is
    // derived from it. Both null for adjustments recorded without a count.
    private Long stockBefore;
    private Long adjustmentIn;
    private Long adjustmentOut;

    // adjustmentIn - adjustmentOut (UI shows "+5" / "-10").
    private Long netAdjustment;
    private Long stockAfter;

    private String adjustedByName;
}
