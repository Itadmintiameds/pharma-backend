package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One row of the Return Summary (by return type) or Return Reason Summary
// table: PHARMACY_TO_WAREHOUSE / "Pharmacy → Warehouse", or DAMAGE / "Damage".
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "label", "returnedQuantity"})
public class StockReturnReportBreakdownRowDto {

    private String code;
    private String label;
    private Long returnedQuantity;
}
