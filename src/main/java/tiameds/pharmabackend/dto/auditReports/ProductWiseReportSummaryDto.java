package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// KPI cards and the Total rows of both tables, over every row matching the
// filters (not just the current page).
@Data
@JsonPropertyOrder({
        "productCount", "lineItemCount",
        "totalPurchaseQuantity", "totalSupplierReturnQuantity", "totalNetPurchaseQuantity",
        "totalSaleQuantity", "totalSalesReturnQuantity", "totalNetSaleQuantity",
        "totalPurchaseAmount", "totalSupplierReturnAmount", "totalNetPurchaseAmount",
        "totalSaleAmount", "totalSalesReturnAmount", "totalNetSaleAmount"})
public class ProductWiseReportSummaryDto {

    // "4 products" badge: distinct products; lineItemCount counts product batches.
    private Long productCount;
    private Long lineItemCount;

    private Long totalPurchaseQuantity;
    private Long totalSupplierReturnQuantity;
    private Long totalNetPurchaseQuantity;
    private Long totalSaleQuantity;
    private Long totalSalesReturnQuantity;
    private Long totalNetSaleQuantity;

    private BigDecimal totalPurchaseAmount;
    private BigDecimal totalSupplierReturnAmount;
    private BigDecimal totalNetPurchaseAmount;
    private BigDecimal totalSaleAmount;
    private BigDecimal totalSalesReturnAmount;
    private BigDecimal totalNetSaleAmount;
}
