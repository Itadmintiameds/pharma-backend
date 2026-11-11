package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Supplier Purchase Return Summary table: its metric rows and the highlighted
// "Total Purchase Return Amount" row, over every row matching the filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "totalPurchaseReturnAmount"})
public class SupplierPurchaseReturnReportReturnSummaryDto {

    private List<SupplierPurchaseReturnReportReturnSummaryRowDto> rows;
    private BigDecimal totalPurchaseReturnAmount;
}
