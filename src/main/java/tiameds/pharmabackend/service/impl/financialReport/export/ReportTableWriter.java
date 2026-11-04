package tiameds.pharmabackend.service.impl.financialReport.export;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

// Writes an exported report: a title block of label / value lines, then the
// table. Implemented for CSV and Excel.
public interface ReportTableWriter extends Closeable {

    void title(String title) throws IOException;

    void line(String label, Object value) throws IOException;

    void blank() throws IOException;

    void header(List<String> headers) throws IOException;

    void row(List<?> values) throws IOException;

    // Completes the file on the output stream.
    void finish() throws IOException;
}
