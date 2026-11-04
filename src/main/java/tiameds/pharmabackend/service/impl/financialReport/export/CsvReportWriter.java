package tiameds.pharmabackend.service.impl.financialReport.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Streams a report as UTF-8 CSV. The byte order mark makes Excel read ₹ and
// non-English names correctly.
class CsvReportWriter implements ReportTableWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Writer writer;


    CsvReportWriter(OutputStream out) throws IOException {
        this.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write('﻿');
    }


    @Override
    public void title(String title) throws IOException {
        writeLine(List.of(title));
    }


    @Override
    public void line(String label, Object value) throws IOException {
        writeLine(List.of(label, format(value)));
    }


    @Override
    public void blank() throws IOException {
        writer.write("\r\n");
    }


    @Override
    public void header(List<String> headers) throws IOException {
        writeLine(headers);
    }


    @Override
    public void row(List<?> values) throws IOException {

        List<String> cells = new ArrayList<>(values.size());

        for (Object value : values) {
            cells.add(format(value));
        }

        writeLine(cells);
    }


    @Override
    public void finish() throws IOException {
        writer.flush();
    }


    // The response stream is closed by the container.
    @Override
    public void close() throws IOException {
        writer.flush();
    }


    private void writeLine(List<String> cells) throws IOException {

        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(cells.get(i)));
        }

        writer.write("\r\n");
    }


    private static String format(Object value) {

        if (value == null) {
            return "";
        }

        if (value instanceof BigDecimal amount) {
            return amount.toPlainString();
        }

        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME);
        }

        if (value instanceof LocalDate date) {
            return date.format(DATE);
        }

        if (value instanceof String text) {
            return neutralizeFormula(text);
        }

        return value.toString();
    }


    // Text starting with = + - @ would run as a formula when the file is
    // opened in a spreadsheet; a leading quote keeps it as text.
    private static String neutralizeFormula(String text) {

        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }

        return text;
    }


    private static String escape(String cell) {

        if (cell.indexOf(',') < 0 && cell.indexOf('"') < 0
                && cell.indexOf('\n') < 0 && cell.indexOf('\r') < 0) {
            return cell;
        }

        return '"' + cell.replace("\"", "\"\"") + '"';
    }
}
