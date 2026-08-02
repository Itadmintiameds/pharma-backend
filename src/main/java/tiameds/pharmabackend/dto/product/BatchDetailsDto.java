package tiameds.pharmabackend.dto.product;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BatchDetailsDto {
    private String batchId;
    private String packagingId;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String purchaseUnit;
    private Long stockQuantity;
    private String freeUnit;
    private Long freeQuantity;
    private Double purchasePrice;
    private Double mrp;
    private Double sellingPrice;
    private Double purchasePricePerUnit;
    private Double mrpPerUnit;
    private Double sellingPricePerUnit;
    private String rackLocation;
}
