package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.AllArgsConstructor;
import lombok.Data;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;

import java.time.LocalDateTime;

// Shared by the bill summary and bill detailed sales reports; K is the
// report's KPI card type and T its row type.
@Data
@AllArgsConstructor
public class BillSummaryReportResponse<K, T> {

    // Name of the user who ran the report, and when (server time).
    private String generatedBy;
    private LocalDateTime generatedOn;

    private K kpi;
    private ReportPageDto<T> billDetails;
}
