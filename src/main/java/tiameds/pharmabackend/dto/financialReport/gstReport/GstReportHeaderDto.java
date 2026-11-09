package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class GstReportHeaderDto {

    private String reportName;

    // Reporting period, inclusive.
    private LocalDate fromDate;
    private LocalDate toDate;

    // ALL, SALES or PURCHASES; ALL, LOCAL or CENTRAL; and the GST rates
    // selected (empty = all), as applied.
    private String transactionType;
    private String gstClassification;
    private List<String> gstRates;

    // One entry per GSTIN among the selected locations.
    private List<GstRegistrationDto> registrations;
}
