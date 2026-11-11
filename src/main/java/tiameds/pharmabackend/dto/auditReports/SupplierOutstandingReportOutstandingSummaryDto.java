package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Supplier Outstanding Summary table (Particulars / Amount), in display
// order, over every invoice matching the filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows"})
public class SupplierOutstandingReportOutstandingSummaryDto {

    private List<SupplierOutstandingReportOutstandingSummaryRowDto> rows;
}
