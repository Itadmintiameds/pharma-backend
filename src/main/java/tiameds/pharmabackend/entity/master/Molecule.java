package tiameds.pharmabackend.entity.master;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_molecules_master")
public class Molecule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "molecule_id")
    private Long moleculeId;

    @Column(name = "molecule_name")
    private String moleculeName;

    @Column(name = "drug_schedule")
    private String drugSchedule;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
