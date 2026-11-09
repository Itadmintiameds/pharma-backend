package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.Data;

import java.math.BigDecimal;

// KPI cards. The sales figures are null when only purchases were asked for,
// the purchase figures when only sales were, and netGst unless both were.
@Data
public class GstSalesPurchaseKpiDto {

    // Net Sale: TOTAL SALES net amount (excluding GST).
    private BigDecimal netSale;

    // Output GST: TOTAL SALES net tax.
    private BigDecimal outputGst;

    // Net Purchase: TOTAL PURCHASES net amount (excluding GST).
    private BigDecimal netPurchase;

    // Input GST: TOTAL PURCHASES net tax.
    private BigDecimal inputGst;

    // Output GST - Input GST.
    private BigDecimal netGst;
}
