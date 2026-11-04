package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One product + batch line of the Product Profitability table.
@Data
@JsonPropertyOrder({
        "pharmacyName", "productId", "productName", "batchNumber",
        "soldQuantity", "returnQuantity", "netQuantity",
        "netSalesAmount", "netCost", "grossProfit", "marginPercentage"})
public class GrossProfitReportRowDto {

    private String pharmacyName;

    private String productId;
    private String productName;
    private String batchNumber;

    // Smallest units, the same unit as bill_quantity.
    private Long soldQuantity;
    private Long returnQuantity;
    private Long netQuantity;

    // Taxable value (after discount, before GST) of sales less returns.
    private BigDecimal netSalesAmount;

    // netQuantity at the batch's purchase price per unit.
    private BigDecimal netCost;

    // netSalesAmount - netCost.
    private BigDecimal grossProfit;

    // grossProfit / netSalesAmount, 0-100.
    private BigDecimal marginPercentage;
}
