package tiameds.pharmabackend.dto.financialReport.billSummary;

import lombok.AllArgsConstructor;
import lombok.Data;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;

@Data
@AllArgsConstructor
public class BillSummaryReportResponse {

    private BillSummaryKpiDto kpi;
    private ReportPageDto<BillSummaryRowDto> billDetails;
}
