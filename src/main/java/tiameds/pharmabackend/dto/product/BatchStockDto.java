package tiameds.pharmabackend.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * A batch row with everything needed to bill against it: the owning product,
 * its packaging (including the smallest unit the stock is counted in), the
 * available stock from pharma_inventory and the batch pricing.
 */
@Data
public class BatchStockDto {

    // ===== batch =====
    private String batchId;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String rackLocation;

    // ===== product =====
    private String productId;
    private String productName;
    private String brandName;
    private BigDecimal gstPercentage;
    private String hsnNo;

    // ===== packaging =====
    private String packagingId;
    private String purchaseUnit;
    private Long purchaseUnitContains;
    private Long purchaseSmallestUnitId;
    private String purchaseSmallestUnitName;

    // ===== stock, counted in smallest units =====
    private Long totalStock;

    // ===== pricing =====
    private Double purchasePrice;
    private Double mrp;
    private Double sellingPrice;
    private Double purchasePricePerUnit;
    private Double mrpPerUnit;
    private Double sellingPricePerUnit;

    // ACTIVE / NEAR_EXPIRY / EXPIRED / OUT_OF_STOCK
    private String status;
}
