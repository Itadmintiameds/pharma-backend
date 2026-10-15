package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

// KPI card figures for the stock adjustment screen, scoped to the caller's
// current warehouse or pharmacy.
@Data
public class StockAdjustmentKpiResponse {

    // Number of stock adjustments.
    private Long totalStockAdjustments;

    // Sum of adjustmentValue across those adjustments. DECREASE values are
    // stored negative, so this is the net change in stock value.
    private Long totalStockValue;
}
