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
@Table(name = "pharma_skin_type_master")
public class SkinType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_type_id")
    private Long skinTypeId;

    @Column(name = "skin_type_name")
    private String skinTypeName;

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
