package tiameds.pharmabackend.service.auditReports;

import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;

public interface GrossProfitSalesBookService {

    // Date-wise (or month-wise) bill amount, cost, gross profit and G.P. %
    // for the period, one page of rows plus totals over the whole filtered set.
    GrossProfitSalesBookResponse getGrossProfitSalesBook(
            GrossProfitSalesBookFilter filter,
            UserDetails user);

    // CSV / Excel download of the report with the same filters: a title
    // block, the KPI totals, then every matching row and a Total row (page
    // and size ignored). format is csv or xlsx.
    ReportExport exportGrossProfitSalesBook(
            GrossProfitSalesBookFilter filter,
            String format,
            UserDetails user);
}
