package tiameds.pharmabackend.dto.purchase;

import lombok.Data;
import tiameds.pharmabackend.enums.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseDto {

    private Long purchaseId;
    private String pharmacyId;
    private String warehouseId;
    private Long supplierId;
    private String supplierName;
    private String grnNo;
    private String invoiceNo;
    private LocalDateTime invoiceDate;
    private BigDecimal invoiceAmount;
    private String paymentType;
    private Long creditDays;
    private String supplierPaymentStatus;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalDiscount;
    private BigDecimal totalGst;
    private BigDecimal totalNetAmount;

    // Server-controlled: ignored on create, always reported on read.
    private ReturnStatus returnStatus;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<PurchaseDetailsDto> purchaseDetails;
}
