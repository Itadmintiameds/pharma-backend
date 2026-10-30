package tiameds.pharmabackend.service.financialReport.pharmacyReceipts;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.PaymentModeSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.PaymentModeSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.ReceiptDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.ReceiptDetailsRowDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.SalesSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.SalesSummaryRowDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface PharmacyReceiptsService {

    FinancialReportResponse<SalesSummaryKpiDto, SalesSummaryRowDto> getSalesSummary(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<PaymentModeSummaryKpiDto, PaymentModeSummaryRowDto> getPaymentModeSummary(
            FinancialReportFilter filter,
            UserDetails user);

    FinancialReportResponse<ReceiptDetailsKpiDto, ReceiptDetailsRowDto> getReceiptDetails(
            FinancialReportFilter filter,
            UserDetails user);
}
