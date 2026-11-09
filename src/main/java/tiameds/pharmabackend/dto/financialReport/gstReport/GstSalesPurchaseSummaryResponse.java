package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// GST Sales & Purchase Summary. Not paged: it has at most a row per month and
// GST rate in each ledger. salesLedger is null when only purchases were asked
// for, purchaseLedger when only sales were, and gstSummary unless both were.
@Data
@AllArgsConstructor
public class GstSalesPurchaseSummaryResponse {

    private String generatedBy;
    private LocalDateTime generatedOn;

    private GstReportHeaderDto header;

    private GstSalesPurchaseKpiDto kpi;

    private GstLedgerDto salesLedger;
    private GstLedgerDto purchaseLedger;

    private GstSummaryDto gstSummary;
}
