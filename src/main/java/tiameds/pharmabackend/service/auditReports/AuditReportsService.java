package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.InventorySalesReportFilter;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface AuditReportsService {

    // Product-wise sales and sales returns for the period, one page of rows
    // plus totals over the whole filtered set.
    InventorySalesReportResponse getInventorySalesReport(
            InventorySalesReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportInventorySalesReport(
            InventorySalesReportFilter filter,
            String format,
            UserDetails user);
}
