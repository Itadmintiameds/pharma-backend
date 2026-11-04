package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportFilter;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface UserWiseCollectionReportService {

    // Billing receipts collected in the period, one page of receipts plus the
    // per-user summary and totals over the whole filtered set.
    UserWiseCollectionReportResponse getUserWiseCollectionReport(
            UserWiseCollectionReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, the user-wise summary, then every matching
    // receipt (page and size ignored). format is csv or xlsx.
    ReportExport exportUserWiseCollectionReport(
            UserWiseCollectionReportFilter filter,
            String format,
            UserDetails user);
}
