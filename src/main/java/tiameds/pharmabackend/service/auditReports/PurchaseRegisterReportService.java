package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportFilter;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface PurchaseRegisterReportService {

    // Supplier purchase invoice lines in the period, one page of rows plus
    // totals over the whole filtered set.
    PurchaseRegisterReportResponse getPurchaseRegisterReport(
            PurchaseRegisterReportFilter filter,
            UserDetails user);
}
