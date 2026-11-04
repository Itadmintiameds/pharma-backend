package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportFilter;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface UserWiseCollectionReportService {

    // Billing receipts collected in the period, one page of receipts plus the
    // per-user summary and totals over the whole filtered set.
    UserWiseCollectionReportResponse getUserWiseCollectionReport(
            UserWiseCollectionReportFilter filter,
            UserDetails user);
}
