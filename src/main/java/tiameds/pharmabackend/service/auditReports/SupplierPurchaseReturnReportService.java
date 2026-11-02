package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface SupplierPurchaseReturnReportService {

    // Line-wise purchase returns to suppliers for the period, one page of rows
    // plus KPI totals over the whole filtered set.
    SupplierPurchaseReturnReportResponse getSupplierPurchaseReturnReport(
            SupplierPurchaseReturnReportFilter filter,
            UserDetails user);
}
