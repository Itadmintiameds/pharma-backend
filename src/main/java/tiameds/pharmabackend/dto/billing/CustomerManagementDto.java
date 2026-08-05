package tiameds.pharmabackend.dto.billing;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerManagementDto {

    private Long customerId;
    private String pharmacyId;
    private String customerName;
    private String customerPhoneNo;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
