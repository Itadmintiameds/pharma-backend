package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface PharmacySalesReturnReportService {

    // Completed sales returns in the period, one page of returned lines plus
    // the per-return settlement and totals over the whole filtered set.
    PharmacySalesReturnReportResponse getPharmacySalesReturnReport(
            PharmacySalesReturnReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, the settlement summary, then every matching
    // returned line (page and size ignored). format is csv or xlsx.
    ReportExport exportPharmacySalesReturnReport(
            PharmacySalesReturnReportFilter filter,
            String format,
            UserDetails user);
}
