package tiameds.pharmabackend.service.financialReport.export;

import java.io.IOException;
import java.io.OutputStream;

// A report download that has been checked and is ready to be written.
// Errors (bad filters, no access, too many rows) are raised before this is
// returned, so they still reach the client as a normal error response.
public interface ReportExport {

    String fileName();

    String contentType();

    // Writes the whole file, reading the report in batches.
    void writeTo(OutputStream out) throws IOException;
}
