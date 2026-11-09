package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One line of the sales or purchase ledger: a month, a GST rate subtotal or
// the ledger total. All amounts exclude GST unless they are tax columns.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GstLedgerRowDto {

    // MONTH, SUBTOTAL or TOTAL.
    private String rowType;

    // "2026-09" on MONTH rows; null on SUBTOTAL / TOTAL.
    private String month;

    // "September 2026", "GST LOCAL SALES 5% - Subtotal", "TOTAL SALES".
    private String label;

    // Net Sale / Net Purc.: amount - crDrNote.
    private BigDecimal netAmount;

    // Net Tax: taxAmount - taxReturn.
    private BigDecimal netTax;

    // Sale Amount / Purc. Amount: taxable value before returns.
    private BigDecimal amount;

    // Tax Amount: GST on that taxable value.
    private BigDecimal taxAmount;

    // CR/DR Note: taxable value of the returns (credit note on sales, debit
    // note on purchases). Separate debit / credit notes are not recorded.
    private BigDecimal crDrNote;

    // Tax Return: GST on the returns.
    private BigDecimal taxReturn;

    // Tax Refund: refunds from the government are not recorded, always zero.
    private BigDecimal taxRefund;
}
