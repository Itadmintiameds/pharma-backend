package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI totals over every row matching the filters (not just the current page).
@Data
@JsonPropertyOrder({"totalSaleAmount", "totalReturnAmount", "totalNetSales", "totalNetSaleQuantity"})
public class InventorySalesReportSummaryDto {

    private BigDecimal totalSaleAmount;
    private BigDecimal totalReturnAmount;
    private BigDecimal totalNetSales;
    private Long totalNetSaleQuantity;
}
