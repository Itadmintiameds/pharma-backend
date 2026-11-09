package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// The sales ledger or the purchase ledger: its sections, then TOTAL SALES /
// TOTAL PURCHASES.
@Data
@AllArgsConstructor
public class GstLedgerDto {

    private List<GstLedgerSectionDto> sections;

    private GstLedgerRowDto total;
}
