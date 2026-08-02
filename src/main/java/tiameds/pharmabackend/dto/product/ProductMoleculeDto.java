package tiameds.pharmabackend.dto.product;

import lombok.Data;

@Data
public class ProductMoleculeDto {
    private String productAttributeId;
    private Long moleculeId;
    private String moleculeStrength;
}
