package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One pharmacy + product row of the Product Sales Details table.
@Data
@JsonPropertyOrder({
        "pharmacyName", "productName",
        "saleQuantity", "returnQuantity", "netSaleQuantity",
        "saleAmount", "returnAmount", "netSales"})
public class InventorySalesReportRowDto {

    private String pharmacyName;
    private String productName;

    private Long saleQuantity;
    private Long returnQuantity;
    private Long netSaleQuantity;

    private BigDecimal saleAmount;
    private BigDecimal returnAmount;
    private BigDecimal netSales;
}
