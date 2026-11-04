package tiameds.pharmabackend.service.impl.financialReport.export;

import java.util.function.Function;

// One column of an exported report: its heading and how to read the value
// from a row. Numbers and dates are kept as such so Excel gets typed cells.
public record ExportColumn<T>(String header, Function<T, ?> value) {

    public static <T> ExportColumn<T> of(String header, Function<T, ?> value) {
        return new ExportColumn<>(header, value);
    }
}
