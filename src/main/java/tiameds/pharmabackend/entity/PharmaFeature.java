package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_feature")
public class PharmaFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_id")
    private Long featureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore
    private PharmaModule module;

    @Column(name = "feature_code", nullable = false, unique = true)
    private String featureCode;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    /**
     * Catalog of permissions applicable to this feature
     * (e.g. PURCHASE_ORDER -> CREATE, VIEW, PRINT, EXPORT).
     * Backed by the join table pharma_feature_permission.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_feature_permission",
            joinColumns = @JoinColumn(name = "feature_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"feature_id", "permission_id"})
    )
    @JsonIgnore
    private List<PharmaPermission> permissions = new ArrayList<>();
}
