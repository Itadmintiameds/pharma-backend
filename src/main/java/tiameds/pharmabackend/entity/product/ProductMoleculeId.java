package tiameds.pharmabackend.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProductMoleculeId implements Serializable {

    @Column(name = "product_attribute_id", length = 30)
    private String productAttributeId;

    @Column(name = "molecule_id")
    private Long moleculeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductMoleculeId that = (ProductMoleculeId) o;
        return Objects.equals(productAttributeId, that.productAttributeId) &&
               Objects.equals(moleculeId, that.moleculeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productAttributeId, moleculeId);
    }
}
