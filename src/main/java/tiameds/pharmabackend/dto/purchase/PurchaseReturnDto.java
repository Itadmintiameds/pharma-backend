package tiameds.pharmabackend.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReturnDto {

    private Long purchaseReturnId;
    private Long purchaseId;
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
    private List<PurchaseReturnDetailsDto> purchaseReturnDetails;
}
