package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface StockReturnReportService {

    // Return lines (pharmacy -> warehouse, pharmacy / warehouse -> supplier)
    // in the period, one page of rows plus, over the whole filtered set: the
    // KPI cards (total quantity and number of returns, damage / expiry
    // quantity and their share), the Return Summary (quantity per return
    // type) and the Return Reason Summary (quantity per reason).
    StockReturnReportResponse getStockReturnReport(
            StockReturnReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, every matching row (page and size ignored),
    // then the Return Summary and Return Reason Summary tables.
    // format is csv or xlsx.
    ReportExport exportStockReturnReport(
            StockReturnReportFilter filter,
            String format,
            UserDetails user);
}
