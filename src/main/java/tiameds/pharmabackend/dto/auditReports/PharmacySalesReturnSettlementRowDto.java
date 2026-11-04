package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;

// One sales return of the Sales Return Settlement Summary table.
// returnAmount = outstandingAdjustment + refundAmount.
@Data
@JsonPropertyOrder({
        "salesReturnId", "salesReturnNo", "customerType",
        "returnAmount", "outstandingAdjustment", "refundAmount"})
public class PharmacySalesReturnSettlementRowDto {

    private Long salesReturnId;
    private String salesReturnNo;
    private String customerType;

    private BigDecimal returnAmount;
    private BigDecimal outstandingAdjustment;
    private BigDecimal refundAmount;
}
