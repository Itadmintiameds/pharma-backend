package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// "SALES LOCAL", "SALES CENTRAL", "PURCHASE LOCAL" or "PURCHASE CENTRAL",
// with one group per GST rate. A section with no transactions has no groups.
@Data
@AllArgsConstructor
public class GstLedgerSectionDto {

    // LOCAL or CENTRAL.
    private String classification;

    private String title;

    private List<GstRateGroupDto> rateGroups;
}
