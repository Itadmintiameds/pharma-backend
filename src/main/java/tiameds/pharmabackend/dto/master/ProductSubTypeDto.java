package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class ProductSubTypeDto {
    private Long productSubTypeId;
    private String productSubTypeName;
    private Long productTypeId;
    private String productTypeName;
    private Boolean isActive;
}
