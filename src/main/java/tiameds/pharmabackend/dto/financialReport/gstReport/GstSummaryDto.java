package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// Net GST per month of the period (every month, zero when nothing happened),
// then the Grand Total.
@Data
@AllArgsConstructor
public class GstSummaryDto {

    private List<GstSummaryRowDto> months;

    private GstSummaryRowDto grandTotal;
}
