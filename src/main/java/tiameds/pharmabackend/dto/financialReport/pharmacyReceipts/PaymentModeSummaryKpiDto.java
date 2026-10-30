package tiameds.pharmabackend.dto.financialReport.pharmacyReceipts;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the payment mode summary. A mode left out by the payment mode
// filter shows zero.
@Data
public class PaymentModeSummaryKpiDto {

    private BigDecimal totalReceived;

    private BigDecimal card;
    private BigDecimal cash;
    private BigDecimal upi;
    private BigDecimal credit;
}
