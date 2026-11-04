package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

import java.util.List;

public interface StockAdjustmentReportService {

    // Stock adjustments (increase / decrease) in the period, one page of rows
    // plus totals over the whole filtered set.
    StockAdjustmentReportResponse getStockAdjustmentReport(
            StockAdjustmentReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportStockAdjustmentReport(
            StockAdjustmentReportFilter filter,
            String format,
            UserDetails user);

    // Distinct reasons recorded on adjustments at the given (or every
    // accessible) pharmacy, for the Adjustment Reason dropdown.
    List<String> getAdjustmentReasons(
            List<String> pharmacyIds,
            UserDetails user);
}
