package tiameds.pharmabackend.service.product.bulk;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw CSV text into the types the product DTOs expect.
 * <p>
 * Every method is tolerant of the shapes these files actually arrive in — numbers
 * that went through a spreadsheet and came back as floats ({@code 3004.0}), dates
 * in the Indian {@code dd/MM/yyyy} order, yes/no booleans, and multi-value cells.
 * A value that cannot be read throws {@link BulkValueException} carrying the column
 * name, so the caller can report it against the right cell instead of failing the file.
 */
final class CellCoercion {

    private CellCoercion() {
    }

    /**
     * Value separators inside one cell. Comma is deliberately excluded: it is the
     * CSV delimiter and appears inside legitimate names (e.g. drug ingredient lists).
     */
    private static final String MULTI_VALUE_SPLIT = "\\s*[|;]\\s*";

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    /**
     * Month-only expiries ("09/2027") are common on packs; they mean end of that month.
     */
    private static final DateTimeFormatter[] MONTH_FORMATS = {
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("M/yyyy"),
            DateTimeFormatter.ofPattern("MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM")
    };

    static String text(CsvFile.Row row, String column) {
        return row.get(column);
    }

    /**
     * Identifier-ish numbers (HSN, licence numbers) that a spreadsheet may have
     * rendered as a float: {@code 3004.0} means {@code 3004}, not {@code 3004.0}.
     */
    static String code(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        if (raw.matches("-?\\d+\\.0+")) {
            return raw.substring(0, raw.indexOf('.'));
        }
        return raw;
    }

    static Double decimal(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        try {
            return Double.valueOf(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            throw new BulkValueException(column, raw, "expected a number");
        }
    }

    static Long integer(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        try {
            // Accept "10" and the spreadsheet's "10.0"; reject a real fraction.
            double d = Double.parseDouble(raw.replace(",", ""));
            if (d != Math.rint(d)) {
                throw new BulkValueException(column, raw, "expected a whole number");
            }
            return (long) d;
        } catch (NumberFormatException e) {
            throw new BulkValueException(column, raw, "expected a whole number");
        }
    }

    static Boolean flag(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase()) {
            case "y", "yes", "true", "1", "sterile", "disposable", "available" -> Boolean.TRUE;
            case "n", "no", "false", "0", "non-sterile", "non sterile",
                 "non-disposable", "non disposable", "unavailable" -> Boolean.FALSE;
            default -> throw new BulkValueException(column, raw, "expected yes/no");
        };
    }

    static LocalDate date(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (java.time.format.DateTimeParseException ignored) {
                // try the next shape
            }
        }
        for (DateTimeFormatter fmt : MONTH_FORMATS) {
            try {
                return YearMonth.parse(raw, fmt).atEndOfMonth();
            } catch (java.time.format.DateTimeParseException ignored) {
                // try the next shape
            }
        }
        throw new BulkValueException(column, raw, "expected a date as dd/MM/yyyy");
    }

    /**
     * Splits a multi-value cell ("Face|Neck") into its parts, empties removed.
     */
    static List<String> multi(CsvFile.Row row, String column) {
        String raw = row.get(column);
        if (raw == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split(MULTI_VALUE_SPLIT)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    /**
     * One entry of a molecule cell: {@code "Paracetamol:500mg|Caffeine:30mg"}.
     * The strength part is optional.
     */
    record MoleculeEntry(String name, String strength) {
    }

    static List<MoleculeEntry> molecules(CsvFile.Row row, String column) {
        List<MoleculeEntry> entries = new ArrayList<>();
        for (String value : multi(row, column)) {
            int split = value.lastIndexOf(':');
            if (split > 0 && split < value.length() - 1) {
                entries.add(new MoleculeEntry(
                        value.substring(0, split).trim(),
                        value.substring(split + 1).trim()));
            } else {
                entries.add(new MoleculeEntry(value, null));
            }
        }
        return entries;
    }

    /**
     * A cell that could not be read. Reported against its row rather than aborting the file.
     */
    static class BulkValueException extends RuntimeException {
        BulkValueException(String column, String value, String expectation) {
            super("Column '" + column + "': could not read \"" + value + "\" (" + expectation + ")");
        }
    }
}
