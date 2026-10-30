package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockMovementReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockMovementReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface StockMovementReportService {

    // Transaction-wise stock movements at pharmacies and warehouses for the
    // period with a running balance, one page of rows plus opening / closing
    // stock and in / out totals over the whole filtered set.
    StockMovementReportResponse getStockMovementReport(
            StockMovementReportFilter filter,
            UserDetails user);
}
