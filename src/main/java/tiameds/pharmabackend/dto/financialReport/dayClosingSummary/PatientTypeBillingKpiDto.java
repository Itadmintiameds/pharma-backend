package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the billing by patient type report: bill count and amount
// per patient type.
@Data
public class PatientTypeBillingKpiDto {

    private PatientTypeTotal ip;
    private PatientTypeTotal op;
    private PatientTypeTotal walkIn;
    private PatientTypeTotal dayCare;


    @Data
    @AllArgsConstructor
    public static class PatientTypeTotal {

        private long billCount;

        private BigDecimal totalAmount;
    }
}
