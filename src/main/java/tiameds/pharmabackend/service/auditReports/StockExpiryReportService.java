package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockExpiryReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface StockExpiryReportService {

    // Batch-wise stock on hand with its expiry status, one page of rows plus
    // totals over the whole filtered set.
    StockExpiryReportResponse getStockExpiryReport(
            StockExpiryReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportStockExpiryReport(
            StockExpiryReportFilter filter,
            String format,
            UserDetails user);
}
