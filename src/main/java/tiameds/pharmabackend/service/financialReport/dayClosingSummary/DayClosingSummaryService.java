package tiameds.pharmabackend.service.financialReport.dayClosingSummary;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingRowDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface DayClosingSummaryService {

    FinancialReportResponse<BillCountSummaryKpiDto, BillCountSummaryRowDto> getBillCountSummary(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<PatientTypeBillingKpiDto, PatientTypeBillingRowDto> getBillingByPatientType(
            FinancialReportFilter filter,
            UserDetails user);
}
