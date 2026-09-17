package tiameds.pharmabackend.service.product.bulk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal RFC-4180 CSV reader, used by the product bulk upload.
 * <p>
 * Written by hand rather than pulling in opencsv/poi because the project has no
 * CSV dependency and the template is a plain comma-separated export. It handles
 * the two things that actually occur in those exports: a UTF-8 BOM on the first
 * header, and quoted fields containing commas (e.g. {@code "NEUROLAB AMP(b1,b6,b12,VITa)"}).
 * Quoted fields may also span newlines, and {@code ""} inside a quoted field is a
 * literal quote.
 * <p>
 * Rows are exposed as header-keyed {@link Row}s so column order in the uploaded
 * file does not have to match the template exactly.
 */
public final class CsvFile {

    private final List<String> headers;
    private final List<Row> rows;

    private CsvFile(List<String> headers, List<Row> rows) {
        this.headers = headers;
        this.rows = rows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<Row> getRows() {
        return rows;
    }

    public boolean hasHeader(String name) {
        return headers.stream().anyMatch(h -> h.equalsIgnoreCase(name));
    }

    public static CsvFile parse(InputStream in) throws IOException {
        List<List<String>> raw = readAll(in);
        if (raw.isEmpty()) {
            return new CsvFile(List.of(), List.of());
        }

        List<String> headers = new ArrayList<>();
        for (String h : raw.get(0)) {
            headers.add(stripBom(h).trim());
        }

        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < raw.size(); i++) {
            List<String> cells = raw.get(i);
            if (isBlankRow(cells)) {
                continue;
            }
            Map<String, String> byHeader = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String value = c < cells.size() ? cells.get(c) : null;
                byHeader.put(key(headers.get(c)), normalize(value));
            }
            // Line number as the operator sees it in a spreadsheet: header is line 1.
            rows.add(new Row(i + 1, byHeader));
        }
        return new CsvFile(headers, rows);
    }

    private static boolean isBlankRow(List<String> cells) {
        return cells.stream().allMatch(c -> c == null || c.isBlank());
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String stripBom(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        // UTF-8 BOM decodes to U+FEFF; a file saved as UTF-8 then read as Latin-1
        // elsewhere shows it as "ï»¿" — we always read UTF-8, so one check is enough.
        return s.charAt(0) == '﻿' ? s.substring(1) : s;
    }

    private static String key(String header) {
        return header == null ? "" : header.trim().toLowerCase();
    }

    /**
     * Streams the whole file into rows of cells, honouring quoting.
     */
    private static List<List<String>> readAll(InputStream in) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            int read;
            while ((read = reader.read()) != -1) {
                char ch = (char) read;

                if (inQuotes) {
                    if (ch == '"') {
                        // A doubled quote is an escaped quote; a single one closes the field.
                        reader.mark(1);
                        int peek = reader.read();
                        if (peek == '"') {
                            cell.append('"');
                        } else {
                            inQuotes = false;
                            if (peek != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        cell.append(ch);
                    }
                    continue;
                }

                switch (ch) {
                    case '"' -> inQuotes = true;
                    case ',' -> {
                        current.add(cell.toString());
                        cell.setLength(0);
                    }
                    case '\r' -> {
                        // swallow; the \n that follows ends the record
                    }
                    case '\n' -> {
                        current.add(cell.toString());
                        cell.setLength(0);
                        rows.add(current);
                        current = new ArrayList<>();
                    }
                    default -> cell.append(ch);
                }
            }
        }

        // Trailing record with no newline at end of file.
        if (cell.length() > 0 || !current.isEmpty()) {
            current.add(cell.toString());
            rows.add(current);
        }
        return rows;
    }

    /**
     * One data row, addressed by column header (case-insensitive).
     */
    public static final class Row {

        private final int lineNumber;
        private final Map<String, String> cells;

        Row(int lineNumber, Map<String, String> cells) {
            this.lineNumber = lineNumber;
            this.cells = new HashMap<>(cells);
        }

        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Trimmed cell value, or null when the column is absent or blank.
         */
        public String get(String header) {
            return cells.get(key(header));
        }

        public boolean isPresent(String header) {
            return get(header) != null;
        }
    }
}
