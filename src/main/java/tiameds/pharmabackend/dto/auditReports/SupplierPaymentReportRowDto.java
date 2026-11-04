package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One supplier payment of the Payment Transactions table.
@Data
@JsonPropertyOrder({
        "supplierPaymentId", "paymentDate", "paymentNumber",
        "purchaseId", "locationName", "supplierName",
        "invoiceNumber", "grnNumber", "invoiceAmount",
        "paymentAmount", "paymentMode", "transactionNumber",
        "outstandingBefore", "outstandingAfter"})
public class SupplierPaymentReportRowDto {

    // For the Supplier Payment No. link.
    private Long supplierPaymentId;

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate paymentDate;

    private String paymentNumber;

    // For the Purchase Invoice No. link.
    private Long purchaseId;

    // Pharmacy or warehouse the purchase was received into.
    private String locationName;
    private String supplierName;
    private String invoiceNumber;
    private String grnNumber;
    private BigDecimal invoiceAmount;
    private BigDecimal paymentAmount;

    // Cash / UPI / Bank Transfer / Cheque / Other.
    private String paymentMode;

    // Reference / Transaction No.; null ("---") when not recorded.
    private String transactionNumber;

    // Invoice amount less earlier payments and purchase returns confirmed up
    // to the payment date, floored at 0.
    private BigDecimal outstandingBefore;

    // outstandingBefore - paymentAmount, floored at 0.
    private BigDecimal outstandingAfter;
}
