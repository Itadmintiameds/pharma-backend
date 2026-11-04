package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface SupplierPaymentReportService {

    // Supplier payments made in the period against credit purchase invoices,
    // one page of rows plus KPI totals and the payment mode-wise summary over
    // the whole filtered set.
    SupplierPaymentReportResponse getSupplierPaymentReport(
            SupplierPaymentReportFilter filter,
            UserDetails user);
}
