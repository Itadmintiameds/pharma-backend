package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PharmaBatchDetailsDto {
    private String batchId;
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
    
    private String createdBy;
    private LocalDateTime createdAt;
}
