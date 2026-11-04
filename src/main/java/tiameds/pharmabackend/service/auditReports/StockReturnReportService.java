package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface StockReturnReportService {

    // Return lines (pharmacy -> warehouse, pharmacy / warehouse -> supplier)
    // in the period, one page of rows plus totals over the whole filtered set.
    StockReturnReportResponse getStockReturnReport(
            StockReturnReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportStockReturnReport(
            StockReturnReportFilter filter,
            String format,
            UserDetails user);
}
