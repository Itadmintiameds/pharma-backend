package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// One date (or month) row of the G.P. on Consolidated Sales Book table,
// summed across the selected pharmacies and net of sales returns.
@Data
@JsonPropertyOrder({"date", "periodLabel", "billAmount", "cost", "grossProfit", "grossProfitPercentage"})
public class GrossProfitSalesBookRowDto {

    // The day, or the first day of the month when grouped by MONTH.
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate date;

    // What the DATE column shows: "01-09-2026", or "Sep-2026" by month.
    private String periodLabel;

    private BigDecimal billAmount;
    private BigDecimal cost;
    private BigDecimal grossProfit;
    private BigDecimal grossProfitPercentage;
}
