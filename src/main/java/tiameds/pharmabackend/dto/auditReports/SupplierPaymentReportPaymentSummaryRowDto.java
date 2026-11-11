package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One line of the Supplier Payment Summary table:
// TOTAL_SUPPLIERS_PAID / "Total Suppliers Paid" / 3.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "value"})
public class SupplierPaymentReportPaymentSummaryRowDto {

    private String code;
    private String label;
    private Long value;
}
