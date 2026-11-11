package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportFilter;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface PurchaseRegisterReportService {

    // Supplier purchase invoice lines in the period, one page of rows plus
    // totals and the Purchase Summary table over the whole filtered set.
    PurchaseRegisterReportResponse getPurchaseRegisterReport(
            PurchaseRegisterReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, every matching row (page and size ignored), then
    // the Purchase Summary table.
    // format is csv or xlsx.
    ReportExport exportPurchaseRegisterReport(
            PurchaseRegisterReportFilter filter,
            String format,
            UserDetails user);
}
