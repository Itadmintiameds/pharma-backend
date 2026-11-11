package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// One line of the Supplier Outstanding Summary table:
// TOTAL_SUPPLIER_PAYABLE / "Total Supplier Payable" / 72000.00 ("₹72,000.00").
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "amount", "highlighted"})
public class SupplierOutstandingReportOutstandingSummaryRowDto {

    private String code;
    private String label;
    private BigDecimal amount;

    // Shaded result row (Total Supplier Payable); it sits mid-table, so the
    // client can't just style the last row.
    private boolean highlighted;
}
