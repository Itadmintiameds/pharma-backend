package tiameds.pharmabackend.dto.product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PharmaProductAttributeDrugDto {
    private String productAttributeId;
    private String drugSchedule;
    private String createdBy;
    private LocalDateTime createdAt;
    
    private List<PharmaProductMoleculeDto> productMolecules;
}
