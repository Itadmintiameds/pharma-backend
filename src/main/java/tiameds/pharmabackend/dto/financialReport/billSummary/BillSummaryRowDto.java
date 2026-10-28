package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One bill in the bill summary report.
@Data
public class BillSummaryRowDto {

    private Long billingId;
    private String billNo;
    private LocalDateTime billedDate;

    private String pharmacyId;
    private String pharmacyName;

    private Long customerId;
    private String patientName;
    private String patientMobileNo;
    private String patientNumber;
    private String opIpNumber;

    private Long doctorId;
    private String referredBy;

    // Number of line items on the bill.
    private Long itemCount;

    // Amount collected: net amount after round off (falls back to net amount
    // for bills saved before round off was recorded).
    private BigDecimal totalAmount;
}
