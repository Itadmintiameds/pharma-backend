package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One line of the billing by patient type report.
@Data
@AllArgsConstructor
public class PatientTypeBillingRowDto {

    // IP, OP, WALK_IN, DAY_CARE or TOTAL.
    private String code;

    // Display label, e.g. Walk-In.
    private String patientType;

    private long billCount;

    private BigDecimal totalAmount;
}
