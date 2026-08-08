package tiameds.pharmabackend.dto.billing;

import lombok.Data;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillingDto {

    private Long billingId;
    private String billNo;
    private String pharmacyId;
    private Long customerId;
    private String customerName;
    private String customerPhoneNo;
    private String customerAddress;
    private String patientNumber;
    private CustomerType customerType;
    private PaymentType paymentType;
    private String opIpNumber;
    private Long doctorId;
    private String doctorName;
    private String prescriptionUrl;
    private BigDecimal totalDiscountPercentage;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalGstAmount;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalNetAmount;
    private String sellingType;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<BillingDetailsDto> billingDetails;
    private List<BillingPaymentDto> billingPayments;
}
