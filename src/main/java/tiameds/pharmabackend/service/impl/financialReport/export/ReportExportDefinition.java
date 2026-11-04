package tiameds.pharmabackend.service.impl.financialReport.export;

import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;
import java.util.function.BiFunction;

/**
 * How a report is exported: its title, the service call that produces it
 * (the same one the report endpoint uses) and its columns.
 *
 * @param businessDay true for day closing reports, which cover one date
 *                    (default today) instead of a range
 */
public record ReportExportDefinition<T>(
        String title,
        boolean businessDay,
        BiFunction<FinancialReportFilter, UserDetails, FinancialReportResponse<?, T>> fetch,
        List<ExportColumn<T>> columns) {
}
