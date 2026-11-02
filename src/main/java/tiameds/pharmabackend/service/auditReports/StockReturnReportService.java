package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface StockReturnReportService {

    // Return lines (pharmacy -> warehouse, pharmacy / warehouse -> supplier)
    // in the period, one page of rows plus totals over the whole filtered set.
    StockReturnReportResponse getStockReturnReport(
            StockReturnReportFilter filter,
            UserDetails user);
}
