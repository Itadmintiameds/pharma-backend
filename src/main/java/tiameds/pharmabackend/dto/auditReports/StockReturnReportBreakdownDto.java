package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Return Summary / Return Reason Summary table: its rows and the Total row,
// over every row matching the filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "totalReturnedQuantity"})
public class StockReturnReportBreakdownDto {

    private List<StockReturnReportBreakdownRowDto> rows;
    private Long totalReturnedQuantity;
}
