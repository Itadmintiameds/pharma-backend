package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One line of the Report Summary table: TOTAL_RETURN_QTY / "Total Return Qty"
// / 12 (COUNT), or TOTAL_GST_REVERSED / "Total GST Reversed" / 39.90
// (AMOUNT, shown as "₹39.90").
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "valueType", "value", "highlighted"})
public class PharmacySalesReturnReportSummaryRowDto {

    public static final String COUNT = "COUNT";
    public static final String AMOUNT = "AMOUNT";

    private String code;
    private String label;

    // COUNT (whole number) or AMOUNT (rupees, 2 decimals).
    private String valueType;
    private BigDecimal value;

    // Shaded result row (Total Return Amount); it sits mid-table, so the
    // client can't just style the last row.
    private boolean highlighted;
}
