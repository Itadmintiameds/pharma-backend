package tiameds.pharmabackend.controller.financialReport.export;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.financialReport.export.ReportExportService;

import java.io.IOException;

// CSV / Excel download of any financial report:
//   /financial-report/export?report=bill-summary&format=xlsx&<the report's filters>
// report is the report's path (bill-summary, current-bill-details, ...);
// format is csv or xlsx. page and size are ignored: every matching row is
// written. With no filters, the report's defaults apply (all dates, or today
// for day closing reports).
@RestController
@RequestMapping("/financial-report")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService reportExportService;

    // Written straight to the response on the request thread, so the file
    // streams out as it is built.
    @GetMapping("/export")
    public void export(
            @RequestParam String report,
            @RequestParam String format,
            @ModelAttribute FinancialReportFilter filter,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response) throws IOException {

        if (currentUser == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        ReportExport export = reportExportService.prepare(report, format, filter, currentUser.getUser());

        response.setContentType(export.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString());

        export.writeTo(response.getOutputStream());
    }
}
