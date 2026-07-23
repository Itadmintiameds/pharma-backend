package tiameds.pharmabackend.dto.product;

import lombok.Data;

@Data
public class PackagingDetailsDto {
    private String packagingId;
    private String purchaseUnit;
    private Long purchaseUnitContains;
    private String smallestUnit;
}
