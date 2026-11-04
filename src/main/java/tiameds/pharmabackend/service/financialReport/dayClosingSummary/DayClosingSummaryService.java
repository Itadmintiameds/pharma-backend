package tiameds.pharmabackend.service.financialReport.dayClosingSummary;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillingByUserKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillingByUserRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.CurrentBillDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.CurrentBillDetailsRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.ReceiptByUserKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.ReceiptByUserRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.SalesReturnDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.SalesReturnDetailsRowDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface DayClosingSummaryService {

    FinancialReportResponse<BillCountSummaryKpiDto, BillCountSummaryRowDto> getBillCountSummary(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<PatientTypeBillingKpiDto, PatientTypeBillingRowDto> getBillingByPatientType(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<BillingByUserKpiDto, BillingByUserRowDto> getBillingByUser(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<ReceiptByUserKpiDto, ReceiptByUserRowDto> getReceiptByUser(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<CurrentBillDetailsKpiDto, CurrentBillDetailsRowDto> getCurrentBillDetails(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<SalesReturnDetailsKpiDto, SalesReturnDetailsRowDto> getSalesReturnDetails(
            FinancialReportFilter filter,
            UserDetails user);
}
