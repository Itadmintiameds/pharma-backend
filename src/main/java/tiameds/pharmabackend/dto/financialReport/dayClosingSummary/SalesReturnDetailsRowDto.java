package tiameds.pharmabackend.dto.financialReport.dayClosingSummary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One completed sales return made on the business day.
@Data
@AllArgsConstructor
public class SalesReturnDetailsRowDto {

    private String pharmacyId;
    private String pharmacyName;

    private Long salesReturnId;
    private String salesReturnNo;

    // The original bill.
    private String billNo;

    // Customer of the original bill; null when it has none.
    private String customerName;

    private LocalDateTime returnedAt;

    private BigDecimal returnAmount;
}
