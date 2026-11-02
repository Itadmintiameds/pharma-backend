package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One purchase return line of the Purchase Return Details table.
@Data
@JsonPropertyOrder({
        "returnDate", "purchaseReturnNo", "purchaseInvoiceNo",
        "supplierName", "locationName",
        "productName", "batchNo", "expiryDate", "returnReason",
        "returnQuantity", "freeReturnQuantity", "returnRate",
        "grossAmount", "discount", "taxableAmount",
        "gstPercentage", "gstAmount", "totalAmount", "status"})
public class SupplierPurchaseReturnReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate returnDate;

    private String purchaseReturnNo;

    // Invoice number of the purchase the goods are going back against.
    private String purchaseInvoiceNo;

    private String supplierName;

    // Pharmacy or warehouse the stock was returned from.
    private String locationName;

    private String productName;
    private String batchNo;

    // Shown as "12/2027".
    @JsonFormat(pattern = "MM/yyyy")
    private LocalDate expiryDate;

    // Reason as entered on the return line: "Damaged", "Expired" ...
    private String returnReason;

    private Long returnQuantity;
    private Long freeReturnQuantity;

    // Gross amount / return quantity.
    private BigDecimal returnRate;

    private BigDecimal grossAmount;
    private BigDecimal discount;

    // grossAmount - discount.
    private BigDecimal taxableAmount;

    // GST slab of the original invoice line: "5%", "18%", "Exempted".
    private String gstPercentage;
    private BigDecimal gstAmount;

    // taxableAmount + gstAmount.
    private BigDecimal totalAmount;

    // DRAFT / CONFIRMED / CANCELLED.
    private String status;
}
