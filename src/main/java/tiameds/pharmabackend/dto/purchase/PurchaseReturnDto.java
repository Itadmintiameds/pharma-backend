package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReturnDto {

    private Long purchaseReturnId;

    // Read-only summary fields. The return has no number/date column of its own,
    // so the id doubles as the return number and createdAt as the return date.
    private String returnNo;
    private LocalDateTime returnDate;

    private Long purchaseId;

    // Supplier and invoice are read off the parent purchase, so the return list
    // can be shown without a second call per row.
    private Long supplierId;
    private String supplierName;
    private String grnNo;
    private String invoiceNo;
    private LocalDateTime invoiceDate;

    private String pharmacyId;
    private String warehouseId;
    private String returnRemarks;
    private Boolean isCancel;
    private String cancelRemark;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalGstAmount;
    private BigDecimal totalNetAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

    // Number of lines on the return, so the list view can show it without
    // counting the items array client-side.
    private Integer itemCount;

    private List<PurchaseReturnDetailsDto> purchaseReturnDetails;
}
