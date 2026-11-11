package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface SupplierPaymentReportService {

    // Supplier payments made in the period against credit purchase invoices,
    // one page of rows plus KPI totals, the Supplier Payment Summary and the
    // Payment Mode-wise Summary over the whole filtered set.
    SupplierPaymentReportResponse getSupplierPaymentReport(
            SupplierPaymentReportFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, the Supplier Payment Summary, the Payment
    // Mode-wise Summary, then every
    // matching row (page and size ignored). format is csv or xlsx.
    ReportExport exportSupplierPaymentReport(
            SupplierPaymentReportFilter filter,
            String format,
            UserDetails user);
}
