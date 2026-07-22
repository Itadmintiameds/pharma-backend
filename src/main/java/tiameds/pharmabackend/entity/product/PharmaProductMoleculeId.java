package tiameds.pharmabackend.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PharmaProductMoleculeId implements Serializable {

    @Column(name = "product_attribute_id")
    private String productAttributeId;

    @Column(name = "molecule_id")
    private Long moleculeId;
}
