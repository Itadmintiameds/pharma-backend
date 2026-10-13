package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.DamagedReturnStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DamagedStockResponse {
    private Long warehouseDistributionDetailsId;
    private String transferNo;
    private LocalDateTime transferDate;
    private String fromStore;
    private String productName;
    private String batchNo;
    private LocalDate expiryDate;
    private String purchaseUnit;
    private Long damagedQty;
    private DamagedReturnStatus stockReturnStatus;
}
