package tiameds.pharmabackend.dto.financialReport.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

// Response of every financial report; K is the report's KPI card type and T
// its row type.
@Data
public class FinancialReportResponse<K, T> {

    // Name of the user who ran the report, and when (server time).
    private String generatedBy;
    private LocalDateTime generatedOn;

    private K kpi;
    private ReportPageDto<T> billDetails;

    // Totals shown below the table, same keys as the KPIs. Only set by reports
    // that have a summary section (bill summary); left out of the JSON
    // otherwise.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private K summary;


    public FinancialReportResponse(String generatedBy, LocalDateTime generatedOn, K kpi, ReportPageDto<T> billDetails) {
        this.generatedBy = generatedBy;
        this.generatedOn = generatedOn;
        this.kpi = kpi;
        this.billDetails = billDetails;
    }
}
