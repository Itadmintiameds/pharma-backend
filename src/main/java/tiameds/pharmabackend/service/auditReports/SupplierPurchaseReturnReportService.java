package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface SupplierPurchaseReturnReportService {

    // Line-wise purchase returns to suppliers for the period, one page of rows
    // plus KPI totals over the whole filtered set.
    SupplierPurchaseReturnReportResponse getSupplierPurchaseReturnReport(
            SupplierPurchaseReturnReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row (page and size ignored).
    // format is csv or xlsx.
    ReportExport exportSupplierPurchaseReturnReport(
            SupplierPurchaseReturnReportFilter filter,
            String format,
            UserDetails user);
}
