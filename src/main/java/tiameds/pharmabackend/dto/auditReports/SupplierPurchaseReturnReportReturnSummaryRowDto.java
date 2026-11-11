package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One metric of the Supplier Purchase Return Summary table:
// RETURN_PURCHASE_QUANTITY / "Return Purchase Quantity" / 65 (COUNT), or
// GST_REVERSAL_AMOUNT / "GST Reversal Amount" / 332.31 (AMOUNT, "₹332.31").
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "valueType", "value"})
public class SupplierPurchaseReturnReportReturnSummaryRowDto {

    public static final String COUNT = "COUNT";
    public static final String AMOUNT = "AMOUNT";

    private String code;
    private String label;

    // COUNT (whole number) or AMOUNT (rupees, 2 decimals).
    private String valueType;
    private BigDecimal value;
}
