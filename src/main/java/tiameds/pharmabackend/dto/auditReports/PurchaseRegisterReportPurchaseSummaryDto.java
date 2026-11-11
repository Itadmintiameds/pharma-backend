package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Purchase Summary table: its metric rows and the highlighted
// "Total Purchase Amount" row, over every row matching the filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "totalPurchaseAmount"})
public class PurchaseRegisterReportPurchaseSummaryDto {

    private List<PurchaseRegisterReportPurchaseSummaryRowDto> rows;
    private BigDecimal totalPurchaseAmount;
}
