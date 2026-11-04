package tiameds.pharmabackend.dto.financialReport.billDueSummary;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One IP bill in the bill due summary report.
@Data
public class BillDueSummaryRowDto {

    private String pharmacyId;
    private String pharmacyName;

    private Long billingId;
    private String billNo;
    private LocalDateTime billedDate;

    private Long customerId;
    private String patientName;
    private String patientMobileNo;
    private String patientNumber;
    private String ipNumber;

    private Long doctorId;
    private String referredBy;

    // Rounded net amount of the bill.
    private BigDecimal totalAmount;

    // Sum of every payment recorded against the bill.
    private BigDecimal amountReceived;

    // totalAmount - amountReceived, never below zero.
    private BigDecimal pendingAmount;

    // PENDING or FULLY_PAID.
    private String status;
}
