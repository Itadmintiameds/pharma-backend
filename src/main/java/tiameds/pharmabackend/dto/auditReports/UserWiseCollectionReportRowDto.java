package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One receipt of the Collection Transaction Details table.
@Data
@JsonPropertyOrder({
        "collectionDate", "paymentId", "receiptNo",
        "billingId", "billNo", "pharmacyName",
        "userId", "userName", "customerType",
        "paymentMode", "transactionId", "collectedAmount"})
public class UserWiseCollectionReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate collectionDate;

    // Receipts have no number of their own; receiptNo is RCP- + payment id.
    private Long paymentId;
    private String receiptNo;

    private Long billingId;
    private String billNo;

    private String pharmacyName;

    private String userId;
    private String userName;

    // WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE ... as stored on the bill.
    private String customerType;

    // CASH, UPI, CARD. transactionId is the UPI / card reference, if any.
    private String paymentMode;
    private String transactionId;

    private BigDecimal collectedAmount;
}
