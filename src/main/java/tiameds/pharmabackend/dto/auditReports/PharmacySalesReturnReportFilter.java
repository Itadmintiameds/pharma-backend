package tiameds.pharmabackend.dto.auditReports;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Query-param filters for the Pharmacy Sales Return Report. Every list is a
// multi-select and every search is a case-insensitive "contains"; null or
// empty means "no restriction" on that field.
@Data
public class PharmacySalesReturnReportFilter {

    // Return date period, both inclusive. Today / Specific Day / By Month /
    // By Date Range on the screen all resolve to this pair on the client.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    // Empty = every pharmacy the logged-in user can access.
    private List<String> pharmacyIds;

    // Customer/Patient Type: WALK_IN, OP_PATIENT, IP_PATIENT, DAYCARE.
    private List<String> customerTypes;

    // SLR-2026-00001
    private String salesReturnNo;

    // Original Sales/Billing bill number.
    private String billNo;

    // Inherited from the original bill's customer.
    private String customerName;
    private String mobileNumber;
    private String patientNumber;

    // Both search the bill's op_ip_number: OP Number on OP Patient bills,
    // IP Number on IP Patient / Daycare bills.
    private String opNumber;
    private String ipNumber;

    // Product Name: pick products by id, and/or free-text search.
    private List<String> productIds;
    private String productName;

    private List<Long> productCategoryIds;

    private String batchNumber;

    // Matched against the reason recorded on the return line, ignoring case.
    private List<String> returnReasons;

    // Historical rate of the returned line (gst / taxable), not the current
    // product master rate.
    private List<BigDecimal> gstPercentages;

    // Ranges on the whole sales return's settlement, both inclusive.
    private BigDecimal minRefundAmount;
    private BigDecimal maxRefundAmount;
    private BigDecimal minOutstandingAdjustment;
    private BigDecimal maxOutstandingAdjustment;

    // 0-based page; offset = page * size.
    private Integer page;
    private Integer size;

    // Print / PDF: every matching row on one page, ignoring page and size.
    // Refused when there are more rows than the print limit.
    private Boolean unpaged;

    // returnDate, salesReturnNo, billNo, pharmacyName, customerType,
    // productName, batchNumber, expiryDate, returnQuantity, returnReason,
    // sellingAmount, discountAmount, taxableAmount, gstAmount,
    // lineReturnAmount. Defaults to returnDate ASC.
    private String sortBy;
    private String sortDir;
}
