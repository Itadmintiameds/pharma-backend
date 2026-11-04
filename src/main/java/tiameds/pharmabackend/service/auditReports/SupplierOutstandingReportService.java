package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface SupplierOutstandingReportService {

    // Credit purchase invoices in the period with what is still payable after
    // supplier payments and confirmed purchase returns, one page of rows plus
    // totals over the whole filtered set.
    SupplierOutstandingReportResponse getSupplierOutstandingReport(
            SupplierOutstandingReportFilter filter,
            UserDetails user);
}
