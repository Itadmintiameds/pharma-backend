package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;
import tiameds.pharmabackend.enums.StockReturnStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarehouseReturnDto {

    private Long warehouseReturnId;
    private String fromPharmacyId;
    private String toWarehouseId;
    private String stockReturnNo;
    private LocalDateTime stockReturnDate;
    private String stockReturnType;
    private StockReturnStatus stockReturnStatus;
    private Long totalReturnProducts;
    private Long totalReturnQuantity;
    private Long totalReceivedQuantity;
    private Long totalNotReceivedQuantity;
    private Boolean isDelete = false;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private List<WarehouseReturnDetailsDto> warehouseReturnDetails;

}
