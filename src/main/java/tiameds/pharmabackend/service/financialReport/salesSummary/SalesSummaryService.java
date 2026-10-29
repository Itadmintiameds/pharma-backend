package tiameds.pharmabackend.service.financialReport.salesSummary;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.salesSummary.SalesSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.salesSummary.SalesSummaryRowDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface SalesSummaryService {

    FinancialReportResponse<SalesSummaryKpiDto, SalesSummaryRowDto> getSalesSummary(
            FinancialReportFilter filter,
            UserDetails user);
}
