package tiameds.pharmabackend.service.financialReport.billSummary;

import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryReportResponse;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.entity.UserDetails;

public interface BillSummaryService {

    BillSummaryReportResponse getBillSummary(FinancialReportFilter filter, UserDetails user);
}
