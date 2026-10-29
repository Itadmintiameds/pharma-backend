package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One product line of a bill in the bill detailed sales report.
@Data
public class BillDetailedSalesRowDto {

    private Long billingId;
    private Long billingDetailsId;
    private String billNo;
    private LocalDateTime billedDate;

    private String pharmacyId;
    private String pharmacyName;

    private Long customerId;
    private String patientName;
    private String patientMobileNo;

    private Long doctorId;
    private String referredBy;

    private String productId;
    private String productName;

    private String batchId;
    private String batchNumber;

    // Quantity billed, in the unit it was billed in.
    private Long quantity;
    private String unit;

    // From the batch's packaging, e.g. Strip / Tablet.
    private String purchaseUnit;
    private String smallestSaleUnit;

    private String hsnNo;

    // The product's current GST rate; bill lines do not store the rate.
    private String gstPercentage;

    private BigDecimal grossAmount;
    private BigDecimal discountAmount;

    // The line's GST amount split equally between central and state.
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;

    private BigDecimal netAmount;
}
