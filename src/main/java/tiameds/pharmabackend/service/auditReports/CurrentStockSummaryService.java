package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryFilter;
import tiameds.pharmabackend.dto.auditReports.CurrentStockSummaryResponse;
import tiameds.pharmabackend.entity.UserDetails;

public interface CurrentStockSummaryService {

    // Batch-wise stock held at pharmacies and warehouses as of a date, one
    // page of rows plus totals over the whole filtered set.
    CurrentStockSummaryResponse getCurrentStockSummary(
            CurrentStockSummaryFilter filter,
            UserDetails user);
}
