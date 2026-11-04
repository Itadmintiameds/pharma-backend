package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.GrossProfitReportFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface GrossProfitReportService {

    // Product + batch-wise net sales against their purchase cost for the
    // period, one page of rows plus totals over the whole filtered set.
    GrossProfitReportResponse getGrossProfitReport(
            GrossProfitReportFilter filter,
            UserDetails user);
}
