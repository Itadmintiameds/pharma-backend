package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.master.Molecule;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_molecule")
public class PharmaProductMolecule {

    @EmbeddedId
    private PharmaProductMoleculeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productAttributeId")
    @JoinColumn(name = "product_attribute_id")
    @JsonIgnore
    private PharmaProductAttributeDrug productAttributeDrug;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("moleculeId")
    @JoinColumn(name = "molecule_id")
    @JsonIgnore
    private Molecule molecule;

    @Column(name = "molecule_strength", length = 100)
    private String moleculeStrength;
}
