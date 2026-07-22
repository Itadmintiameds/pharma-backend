package tiameds.pharmabackend.entity.master;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "therapeutic_category_master")
public class TherapeuticCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "therapeutic_category_id")
    private Long therapeuticCategoryId;

    @Column(name = "therapeutic_category_name")
    private String therapeuticCategoryName;

    @OneToMany(mappedBy = "therapeuticCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<TherapeuticSubcategory> therapeuticSubcategories;

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
