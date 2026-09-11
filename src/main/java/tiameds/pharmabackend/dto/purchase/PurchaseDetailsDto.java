package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseDetailsDto {

    private Long purchaseDetailsId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private Long purchaseQuantity;
    private String freeUnit;
    private Long freeQuantity;
    private BigDecimal grossAmount;
    private BigDecimal gst;
    // The product's GST slab at the time of purchase — "5%", "28%", or the
    // non-numeric "Exempted" slab, not a plain rate.
    private String gstPercentage;
    private BigDecimal netAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
