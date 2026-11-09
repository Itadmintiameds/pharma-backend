package tiameds.pharmabackend.service.financialReport.gstReport;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstRegistrationDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstSalesPurchaseSummaryResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.util.List;

public interface GstReportService {

    GstSalesPurchaseSummaryResponse getSalesPurchaseSummary(FinancialReportFilter filter, UserDetails user);

    // GSTINs of the selected (or default) pharmacies / warehouses, for the
    // GSTIN dropdown.
    List<GstRegistrationDto> getGstRegistrations(FinancialReportFilter filter, UserDetails user);

    // CSV / Excel of the summary: the header, KPIs, then the three sections.
    ReportExport exportSalesPurchaseSummary(String format, FinancialReportFilter filter, UserDetails user);
}
