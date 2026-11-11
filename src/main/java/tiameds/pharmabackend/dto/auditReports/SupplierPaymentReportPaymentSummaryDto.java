package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Supplier Payment Summary table (Particulars / Value): its count rows and the
// highlighted "Total Payment Amount" row, over every payment matching the
// filters.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rows", "totalPaymentAmount"})
public class SupplierPaymentReportPaymentSummaryDto {

    private List<SupplierPaymentReportPaymentSummaryRowDto> rows;
    private BigDecimal totalPaymentAmount;
}
