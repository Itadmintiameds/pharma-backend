package tiameds.pharmabackend.service.impl.financialReport.export;

import java.io.IOException;
import java.io.OutputStream;

// Download formats of the report export.
enum ExportFormat {

    CSV("text/csv; charset=UTF-8", ".csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String extension;


    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }


    // csv, or xlsx / excel.
    static ExportFormat parse(String value) {

        String format = value == null ? "" : value.trim().toLowerCase();

        return switch (format) {
            case "csv" -> CSV;
            case "xlsx", "excel" -> XLSX;
            default -> throw new IllegalArgumentException("Unknown format: " + value + ". Use csv or xlsx.");
        };
    }


    ReportTableWriter open(OutputStream out) throws IOException {
        return this == CSV ? new CsvReportWriter(out) : new ExcelReportWriter(out);
    }


    String contentType() {
        return contentType;
    }


    String extension() {
        return extension;
    }
}
