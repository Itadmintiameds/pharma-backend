package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One line of the Report Summary table: NET_PURCHASE_QTY / "Net Purchase Qty"
// / 2405 (COUNT), or NET_SALES_AMOUNT / "Net Sales Amount" / 99500.00
// (AMOUNT, shown as "₹99,500.00").
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "valueType", "value"})
public class ProductWiseReportSummaryRowDto {

    public static final String COUNT = "COUNT";
    public static final String AMOUNT = "AMOUNT";

    private String code;
    private String label;

    // COUNT (whole number) or AMOUNT (rupees, 2 decimals).
    private String valueType;
    private BigDecimal value;
}
