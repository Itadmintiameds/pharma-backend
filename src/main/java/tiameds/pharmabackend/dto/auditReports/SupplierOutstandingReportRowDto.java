package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One credit purchase invoice of the Outstanding Invoices table.
@Data
@JsonPropertyOrder({
        "purchaseId", "locationName", "supplierName",
        "invoiceNumber", "grnNumber", "invoiceDate", "dueDate",
        "invoiceAmount", "totalPaid", "purchaseReturnAdjusted",
        "currentOutstanding", "supplierCredit", "overdueDays", "outstandingStatus"})
public class SupplierOutstandingReportRowDto {

    // For the Purchase Invoice No. link.
    private Long purchaseId;

    // Pharmacy or warehouse the purchase was received into.
    private String locationName;
    private String supplierName;
    private String invoiceNumber;
    private String grnNumber;

    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate invoiceDate;

    // invoiceDate + credit days; shown in red once overdue.
    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate dueDate;

    private BigDecimal invoiceAmount;

    // Sum of supplier payments against the invoice.
    private BigDecimal totalPaid;

    // Sum of confirmed purchase returns against the invoice.
    private BigDecimal purchaseReturnAdjusted;

    // invoiceAmount - totalPaid - purchaseReturnAdjusted, floored at 0.
    private BigDecimal currentOutstanding;

    // What payments and returns push past the invoice amount; 0 otherwise.
    private BigDecimal supplierCredit;

    // Days past the due date while still outstanding; null ("—") otherwise.
    private Long overdueDays;

    // Unpaid / Partially Settled / Fully Settled.
    private String outstandingStatus;
}
