package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.ProductWiseReportFilter;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface ProductWiseReportService {

    // Purchases, supplier returns, sales and sales returns per product batch
    // for the period, one page of rows plus totals over the whole filtered set.
    ProductWiseReportResponse getProductWiseReport(
            ProductWiseReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching product batch (page and size
    // ignored). format is csv or xlsx.
    ReportExport exportProductWiseReport(
            ProductWiseReportFilter filter,
            String format,
            UserDetails user);
}
