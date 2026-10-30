package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.InventorySalesReportFilter;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface AuditReportsService {

    // Product-wise sales and sales returns for the period, one page of rows
    // plus totals over the whole filtered set.
    InventorySalesReportResponse getInventorySalesReport(
            InventorySalesReportFilter filter,
            UserDetails user);
}
