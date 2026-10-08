package tiameds.pharmabackend.dto.billing;

import lombok.Data;

import java.math.BigDecimal;

// KPI card figures for the sales return screen, scoped to the selected
// pharmacy. Both count only Completed returns.
@Data
public class SalesReturnKpiResponse {

    // Number of sales returns.
    private Long totalSalesReturns;

    // Sum of totalNetAmount across those returns — what was refunded.
    private BigDecimal totalReturnAmount;
}
