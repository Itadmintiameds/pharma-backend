package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One month of the GST summary, or its Grand Total.
@Data
@AllArgsConstructor
public class GstSummaryRowDto {

    // MONTH or TOTAL.
    private String rowType;

    // "2026-09" on MONTH rows; null on the total.
    private String month;

    // "September 2026" or "Grand Total".
    private String label;

    // Net Tax of the sales ledger (Local + Central).
    private BigDecimal outputGst;

    // Net Tax of the purchase ledger (Local + Central).
    private BigDecimal inputGst;

    // outputGst - inputGst; negative when more GST was paid than collected.
    private BigDecimal netGst;
}
