package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryFilter;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface CurrentStockSummaryService {

    // Batch-wise stock held at pharmacies and warehouses as of a date, one
    // page of rows plus totals over the whole filtered set.
    CurrentStockSummaryResponse getCurrentStockSummary(
            CurrentStockSummaryFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportCurrentStockSummary(
            CurrentStockSummaryFilter filter,
            String format,
            UserDetails user);
}
