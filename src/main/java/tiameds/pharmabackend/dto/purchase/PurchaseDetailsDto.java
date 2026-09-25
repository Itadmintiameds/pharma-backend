package tiameds.pharmabackend.dto.purchase;

import lombok.Data;
import tiameds.pharmabackend.enums.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseDetailsDto {

    private Long purchaseDetailsId;
    private String productId;
    private String productName;
    private String batchId;
    private String batchNumber;
    private LocalDate expiryDate;

    // Read off the product's packaging: the unit the product is bought in, the
    // smallest unit it is sold in, and how many of the latter one of the former
    // holds (e.g. "Strip" of "Tablet" x 10).
    private String purchaseUnit;
    private String smallestUnit;
    private Long unitContains;

    private Long purchaseQuantity;
    private String freeUnit;
    private Long freeQuantity;
    private BigDecimal grossAmount;
    private BigDecimal gst;
    // The product's GST slab at the time of purchase — "5%", "28%", or the
    // non-numeric "Exempted" slab, not a plain rate.
    private String gstPercentage;
    private BigDecimal netAmount;

    // Server-controlled: ignored on create, always reported on read.
    private ReturnStatus returnDetailsStatus;

    // Stock this batch has left right now at the location that raised the
    // purchase, in smallest units. Not a property of the purchase — it is read
    // live from inventory, so it moves as the batch is sold or returned.
    private Long availableStock;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
