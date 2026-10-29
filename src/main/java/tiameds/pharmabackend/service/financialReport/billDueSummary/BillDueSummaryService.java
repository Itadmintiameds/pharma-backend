package tiameds.pharmabackend.service.financialReport.billDueSummary;

import tiameds.pharmabackend.dto.financialReport.billDueSummary.BillDueSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.billDueSummary.BillDueSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface BillDueSummaryService {

    FinancialReportResponse<BillDueSummaryKpiDto, BillDueSummaryRowDto> getBillDueSummary(
            FinancialReportFilter filter,
            UserDetails user);
}
