package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.GrossProfitReportFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface GrossProfitReportService {

    // Product + batch-wise net sales against their purchase cost for the
    // period, one page of rows plus totals over the whole filtered set.
    GrossProfitReportResponse getGrossProfitReport(
            GrossProfitReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportGrossProfitReport(
            GrossProfitReportFilter filter,
            String format,
            UserDetails user);
}
