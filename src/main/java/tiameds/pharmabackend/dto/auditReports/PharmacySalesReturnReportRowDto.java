package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One returned product + batch line of the Sales Return Transactions table.
@Data
@JsonPropertyOrder({
        "returnDate", "salesReturnId", "salesReturnNo", "billNo",
        "pharmacyName", "customerType",
        "productName", "batchNumber", "expiryDate",
        "returnQuantity", "returnReason",
        "sellingAmount", "discountAmount", "taxableAmount", "gstAmount",
        "lineReturnAmount"})
public class PharmacySalesReturnReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate returnDate;

    private Long salesReturnId;
    private String salesReturnNo;
    private String billNo;

    private String pharmacyName;

    // WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE ... as stored on the bill.
    private String customerType;

    private String productName;
    private String batchNumber;

    @JsonFormat(pattern = "MM/yyyy", locale = "en")
    private LocalDate expiryDate;

    private Long returnQuantity;
    private String returnReason;

    // Historical amount = taxable + discount reversed.
    private BigDecimal sellingAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal gstAmount;

    // taxable + gst, as recorded on the return line.
    private BigDecimal lineReturnAmount;
}
