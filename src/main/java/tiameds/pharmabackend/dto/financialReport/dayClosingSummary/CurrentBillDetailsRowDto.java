package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One bill raised on the business day.
@Data
@AllArgsConstructor
public class CurrentBillDetailsRowDto {

    private String pharmacyId;
    private String pharmacyName;

    private Long billingId;
    private String billNo;

    // Name captured against the bill; null when the bill has no customer.
    private String customerName;

    // IP, OP, Walk-In, Day Care, or the stored type for any other.
    private String patientType;

    private LocalDateTime billedAt;

    // GST-inclusive amount after discount (rounded net amount).
    private BigDecimal totalAmount;

    // Received on the bill up to the end of the business day.
    private BigDecimal amountReceived;

    // Completed returns against the bill up to the end of the business day.
    private BigDecimal returnedAmount;

    // totalAmount - amountReceived - returnedAmount (never below zero) for IP
    // bills; zero for every other type.
    private BigDecimal pendingAmount;
}
