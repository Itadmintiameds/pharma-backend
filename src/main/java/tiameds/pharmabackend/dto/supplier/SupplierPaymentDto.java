package tiameds.pharmabackend.dto.supplier;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SupplierPaymentDto {

    private Long supplierPaymentId;
    private Long purchaseId;
    private LocalDate paymentDate;
    private String paymentMode;
    private String referenceNumber;
    private Double paidAmount;
    private Double outstandingAmount;
    private Boolean isActive;
    private String createdBy;
    private String createdAt;
    private String modifiedBy;
    private String modifiedAt;
}
