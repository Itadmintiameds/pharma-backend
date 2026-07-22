package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PharmaPackagingDetailsDto {
    private String packagingId;
    private String purchaseUnit;
    private Long purchaseUnitContains;
    private String smallestUnit;
    
    private String createdBy;
    private LocalDateTime createdAt;
}
