package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One purchase invoice line of the Purchase Details table.
@Data
@JsonPropertyOrder({
        "purchaseDate", "invoiceNumber", "supplierName", "locationName",
        "productName", "batchNo", "expiryDate",
        "purchasedQuantity", "freeQuantity", "purchaseRate",
        "grossAmount", "discount", "taxableAmount",
        "gstPercentage", "gstAmount", "totalAmount"})
public class PurchaseRegisterReportRowDto {

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate purchaseDate;

    private String invoiceNumber;
    private String supplierName;

    // Pharmacy or warehouse the purchase was received into.
    private String locationName;

    private String productName;
    private String batchNo;

    // Shown as "12/2027".
    @JsonFormat(pattern = "MM/yyyy")
    private LocalDate expiryDate;

    private Long purchasedQuantity;
    private Long freeQuantity;

    // Gross amount / purchased quantity.
    private BigDecimal purchaseRate;

    private BigDecimal grossAmount;
    private BigDecimal discount;

    // grossAmount - discount.
    private BigDecimal taxableAmount;

    // GST slab as recorded on the invoice line: "5%", "18%", "Exempted".
    private String gstPercentage;
    private BigDecimal gstAmount;

    // taxableAmount + gstAmount.
    private BigDecimal totalAmount;
}
