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

    // Read-only, computed on every GET from live supplier payments and
    // confirmed purchase returns (not stored; ignored on create).
    // Credit purchase: outstanding = totalNetAmount - paid - returned, floored
    // at 0; the excess below 0 is supplierCreditAmount. Other purchases were
    // paid upfront: outstanding 0, returns are supplier credit.
    private BigDecimal paidAmount;
    private BigDecimal returnedAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal supplierCreditAmount;

    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<PurchaseDetailsDto> purchaseDetails;
}
