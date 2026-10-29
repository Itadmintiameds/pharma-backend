package tiameds.pharmabackend.dto.financialReport.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

// Response of every financial report; K is the report's KPI card type and T
// its row type.
@Data
@AllArgsConstructor
public class FinancialReportResponse<K, T> {

    // Name of the user who ran the report, and when (server time).
    private String generatedBy;
    private LocalDateTime generatedOn;

    private K kpi;
    private ReportPageDto<T> billDetails;
}
