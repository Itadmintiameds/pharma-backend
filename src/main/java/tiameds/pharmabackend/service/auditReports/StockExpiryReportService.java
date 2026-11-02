package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockExpiryReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface StockExpiryReportService {

    // Batch-wise stock on hand with its expiry status, one page of rows plus
    // totals over the whole filtered set.
    StockExpiryReportResponse getStockExpiryReport(
            StockExpiryReportFilter filter,
            UserDetails user);
}
