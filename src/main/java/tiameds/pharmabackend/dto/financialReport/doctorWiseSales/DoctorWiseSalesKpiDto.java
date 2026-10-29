package tiameds.pharmabackend.dto.financialReport.doctorWiseSales;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards for the doctor-wise sales report. Totals of every row matching the
// filters, not just the current page.
@Data
public class DoctorWiseSalesKpiDto {

    private BigDecimal totalSaleValue;
    private BigDecimal totalReturnValue;

    // totalSaleValue - totalReturnValue.
    private BigDecimal netSaleValue;
}
