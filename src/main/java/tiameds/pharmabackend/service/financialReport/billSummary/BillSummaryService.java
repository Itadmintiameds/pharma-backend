package tiameds.pharmabackend.service.financialReport.billSummary;

import tiameds.pharmabackend.dto.financialReport.billSummary.BillDetailedSalesKpiDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillDetailedSalesRowDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryReportResponse;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.entity.UserDetails;

public interface BillSummaryService {

    BillSummaryReportResponse<BillSummaryKpiDto, BillSummaryRowDto> getBillSummary(
            FinancialReportFilter filter,
            UserDetails user);

    BillSummaryReportResponse<BillDetailedSalesKpiDto, BillDetailedSalesRowDto> getBillDetailedSales(
            FinancialReportFilter filter,
            UserDetails user);
}
