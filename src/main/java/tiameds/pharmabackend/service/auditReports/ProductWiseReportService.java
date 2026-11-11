package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.ProductWiseReportFilter;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface ProductWiseReportService {

    // Purchases, supplier returns, sales and sales returns per product batch
    // for the period, one page of rows plus totals and the Report Summary
    // table over the whole filtered set.
    ProductWiseReportResponse getProductWiseReport(
            ProductWiseReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, every matching product batch (page and size
    // ignored), then the Report Summary table. format is csv or xlsx.
    ReportExport exportProductWiseReport(
            ProductWiseReportFilter filter,
            String format,
            UserDetails user);
}
