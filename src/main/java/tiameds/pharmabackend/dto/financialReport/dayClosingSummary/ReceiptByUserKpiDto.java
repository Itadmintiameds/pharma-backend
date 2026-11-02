package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the receipt by user report.
@Data
public class ReceiptByUserKpiDto {

    // Payment transactions recorded on the day, across all users shown.
    private long totalReceipts;

    private BigDecimal totalAmountReceived;
}
