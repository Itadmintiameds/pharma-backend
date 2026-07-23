package tiameds.pharmabackend.dto.supplier;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierMasterDto {

    private Long supplierId;
    private String pharmacyId;
    private String supplierName;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
