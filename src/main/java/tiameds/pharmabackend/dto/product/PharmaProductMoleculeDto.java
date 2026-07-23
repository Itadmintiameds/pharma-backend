package tiameds.pharmabackend.dto.product;

import lombok.Data;

@Data
public class PharmaProductMoleculeDto {
    private String productAttributeId;
    private Long moleculeId;
    private String moleculeStrength;
}
