package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockMovementReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface StockMovementReportService {

    // Transaction-wise stock movements at pharmacies and warehouses for the
    // period with a running balance, one page of rows plus opening / closing
    // stock and in / out totals over the whole filtered set.
    StockMovementReportResponse getStockMovementReport(
            StockMovementReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportStockMovementReport(
            StockMovementReportFilter filter,
            String format,
            UserDetails user);
}
