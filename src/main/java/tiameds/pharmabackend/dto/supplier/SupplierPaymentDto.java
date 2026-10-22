package tiameds.pharmabackend.dto.supplier;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SupplierPaymentDto {

    private Long supplierPaymentId;
    private Long supplierId;
    private Long purchaseId;
    private String supplierPaymentNo;
    private LocalDate supplierPaymentDate;
    private String paymentMode;
    private Double paymentAmount;
    private String transactionNo;
    private String remarks;
    private String createdBy;
    private String createdAt;
    private String modifiedBy;
    private String modifiedAt;
}
