package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// One GST rate within a Local / Central section, e.g. "GST LOCAL SALES 5%":
// a row per month with transactions, then the rate's subtotal.
@Data
@AllArgsConstructor
public class GstRateGroupDto {

    private BigDecimal gstRate;

    private String title;

    private List<GstLedgerRowDto> months;

    private GstLedgerRowDto subtotal;
}
