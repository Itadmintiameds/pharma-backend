package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface SupplierOutstandingReportService {

    // Credit purchase invoices in the period with what is still payable after
    // supplier payments and confirmed purchase returns, one page of rows plus
    // totals and the Supplier Outstanding Summary table over the whole
    // filtered set.
    SupplierOutstandingReportResponse getSupplierOutstandingReport(
            SupplierOutstandingReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, every matching row (page and size ignored), then
    // the Supplier Outstanding Summary table.
    // format is csv or xlsx.
    ReportExport exportSupplierOutstandingReport(
            SupplierOutstandingReportFilter filter,
            String format,
            UserDetails user);
}
