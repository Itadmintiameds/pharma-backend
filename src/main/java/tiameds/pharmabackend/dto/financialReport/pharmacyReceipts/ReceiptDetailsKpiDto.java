package tiameds.pharmabackend.dto.financialReport.pharmacyReceipts;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the receipt details report.
@Data
public class ReceiptDetailsKpiDto {

    // currentBillReceipts + previousBillReceipts.
    private BigDecimal totalReceipts;

    // Received in the period on bills raised in the period.
    private BigDecimal currentBillReceipts;

    // Received in the period on bills raised before it.
    private BigDecimal previousBillReceipts;
}
