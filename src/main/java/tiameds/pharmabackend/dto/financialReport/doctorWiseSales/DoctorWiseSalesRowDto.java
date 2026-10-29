package tiameds.pharmabackend.dto.financialReport.doctorWiseSales;

import lombok.Data;

import java.math.BigDecimal;

// One product sold on bills referred by one doctor, in one pharmacy.
// Quantities are in the smallest sale unit, as stored on the bill.
@Data
public class DoctorWiseSalesRowDto {

    private String pharmacyId;
    private String pharmacyName;

    private Long doctorId;
    private String referredBy;

    private String productId;
    private String productName;

    private Long saleQuantity;
    private Long returnQuantity;

    private BigDecimal saleValue;
    private BigDecimal returnValue;

    // saleValue - returnValue.
    private BigDecimal netSaleValue;
}
