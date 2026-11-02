package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockDamageReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface StockDamageReportService {

    // Damaged stock handled through Stock Return and Stock Adjustment in the
    // period, one page of rows plus totals over the whole filtered set.
    StockDamageReportResponse getStockDamageReport(
            StockDamageReportFilter filter,
            UserDetails user);
}
