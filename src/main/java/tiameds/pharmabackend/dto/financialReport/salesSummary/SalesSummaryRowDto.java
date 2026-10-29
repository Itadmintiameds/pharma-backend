package tiameds.pharmabackend.dto.financialReport.salesSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

// One line of the sales summary: a single cumulative figure across the
// selected pharmacies and period.
@Data
@AllArgsConstructor
public class SalesSummaryRowDto {

    // Stable key for the frontend, e.g. TOTAL_SALES.
    private String code;

    // Display label, e.g. Total Sales.
    private String metric;

    // Where the figure comes from, e.g. Pharmacy Bill.
    private String source;

    // How it is calculated.
    private String calculation;

    private BigDecimal amount;
}
