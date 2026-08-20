package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WarehouseDistributionLineResponse {

    private Long warehouseDistributionDetailsId;
    private String productId;
    private String packagingId;
    private String batchId;
    private Long issueQuantity;
    private Long receivedQuantity;
    private Long damagedQuantity;
    private String remarks;

    // Nested detail objects so the frontend can render each line without extra lookups.
    private ProductInfo product;
    private PackagingInfo packaging;
    private BatchInfo batch;

    @Data
    public static class ProductInfo {
        private String productId;
        private String productName;
        private String brandName;
        private String hsnNo;
        private BigDecimal gstPercentage;
    }

    @Data
    public static class PackagingInfo {
        private String packagingId;
        private String purchaseUnit;
        private Long purchaseUnitContains;
    }

    @Data
    public static class BatchInfo {
        private String batchId;
        private String batchNumber;
        private LocalDate manufacturingDate;
        private LocalDate expiryDate;
        private Double mrp;
        private Double sellingPrice;
        private Double purchasePrice;
        private String rackLocation;
    }
}
