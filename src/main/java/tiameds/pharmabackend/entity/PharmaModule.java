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
@Table(name = "pharma_module")
public class PharmaModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "module_name", nullable = false, unique = true)
    private String moduleName;

    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PharmaFeature> features = new ArrayList<>();
}
