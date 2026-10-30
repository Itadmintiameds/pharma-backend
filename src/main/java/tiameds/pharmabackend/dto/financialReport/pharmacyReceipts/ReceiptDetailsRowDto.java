package tiameds.pharmabackend.dto.financialReport.pharmacyReceipts;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One line of the receipt details report.
@Data
@AllArgsConstructor
public class ReceiptDetailsRowDto {

    // CURRENT_BILL_RECEIPTS, PREVIOUS_BILL_RECEIPTS or TOTAL_RECEIPTS.
    private String code;

    // Display label, e.g. Current Bill Receipts.
    private String particular;

    // Where the figure comes from.
    private String source;

    // How it is calculated.
    private String calculation;

    private BigDecimal amount;
}
