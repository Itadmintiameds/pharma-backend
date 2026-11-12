package tiameds.pharmabackend.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tiameds.pharmabackend.enums.SalesReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One sales return raised against a bill, shown on the bill (read-only).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingSalesReturnDto {

    private Long salesReturnId;
    private String salesReturnNo;
    private LocalDateTime salesReturnDate;
    private SalesReturnStatus status;

    // The return's total net amount (what was refunded / credited).
    private BigDecimal returnAmount;
}
