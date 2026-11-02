package tiameds.pharmabackend.service.financialReport.export;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.entity.UserDetails;

public interface ReportExportService {

    /**
     * @param report the report's path, e.g. bill-summary
     * @param format csv or xlsx
     */
    ReportExport prepare(String report, String format, FinancialReportFilter filter, UserDetails user);
}
