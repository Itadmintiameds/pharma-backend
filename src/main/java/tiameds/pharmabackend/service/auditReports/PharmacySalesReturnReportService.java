package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface PharmacySalesReturnReportService {

    // Completed sales returns in the period, one page of returned lines plus
    // the per-return settlement and totals over the whole filtered set.
    PharmacySalesReturnReportResponse getPharmacySalesReturnReport(
            PharmacySalesReturnReportFilter filter,
            UserDetails user);
}
