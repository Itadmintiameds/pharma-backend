package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One metric of the Purchase Summary table: PURCHASE_INVOICES /
// "Purchase Invoices" / 4 (COUNT), or GST_AMOUNT / "GST Amount" / 1491.50
// (AMOUNT, shown as "₹1,491.50").
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "valueType", "value"})
public class PurchaseRegisterReportPurchaseSummaryRowDto {

    public static final String COUNT = "COUNT";
    public static final String AMOUNT = "AMOUNT";

    private String code;
    private String label;

    // COUNT (whole number) or AMOUNT (rupees, 2 decimals).
    private String valueType;
    private BigDecimal value;
}
