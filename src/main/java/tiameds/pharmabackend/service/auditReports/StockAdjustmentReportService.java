package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface StockAdjustmentReportService {

    // Stock adjustments (increase / decrease) in the period, one page of rows
    // plus totals over the whole filtered set.
    StockAdjustmentReportResponse getStockAdjustmentReport(
            StockAdjustmentReportFilter filter,
            UserDetails user);

    // Distinct reasons recorded on adjustments at the given (or every
    // accessible) pharmacy, for the Adjustment Reason dropdown.
    List<String> getAdjustmentReasons(
            List<String> pharmacyIds,
            UserDetails user);
}
