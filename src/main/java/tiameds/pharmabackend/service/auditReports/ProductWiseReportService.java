package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.ProductWiseReportFilter;
import tiameds.pharmabackend.dto.auditReports.ProductWiseReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface ProductWiseReportService {

    // Purchases, supplier returns, sales and sales returns per product batch
    // for the period, one page of rows plus totals over the whole filtered set.
    ProductWiseReportResponse getProductWiseReport(
            ProductWiseReportFilter filter,
            UserDetails user);
}
