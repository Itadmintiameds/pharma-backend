package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One product batch: feeds both the Quantity Analysis and the Purchase and
// Sales Value Analysis tables. Quantities are in stock (smallest) units, so
// purchase and sales sides are comparable.
@Data
@JsonPropertyOrder({
        "productId", "productName", "batchNo",
        "purchaseQuantity", "supplierReturnQuantity", "netPurchaseQuantity",
        "saleQuantity", "salesReturnQuantity", "netSaleQuantity",
        "purchaseAmount", "supplierReturnAmount", "netPurchaseAmount",
        "saleAmount", "salesReturnAmount", "netSaleAmount"})
public class ProductWiseReportRowDto {

    private String productId;
    private String productName;
    private String batchNo;

    private Long purchaseQuantity;
    private Long supplierReturnQuantity;
    private Long netPurchaseQuantity;
    private Long saleQuantity;
    private Long salesReturnQuantity;
    private Long netSaleQuantity;

    private BigDecimal purchaseAmount;
    private BigDecimal supplierReturnAmount;
    private BigDecimal netPurchaseAmount;
    private BigDecimal saleAmount;
    private BigDecimal salesReturnAmount;
    private BigDecimal netSaleAmount;
}
