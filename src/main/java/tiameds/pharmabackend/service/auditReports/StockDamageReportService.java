package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockDamageReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface StockDamageReportService {

    // Damaged stock handled through Stock Return and Stock Adjustment in the
    // period, one page of rows plus totals over the whole filtered set.
    StockDamageReportResponse getStockDamageReport(
            StockDamageReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportStockDamageReport(
            StockDamageReportFilter filter,
            String format,
            UserDetails user);
}
