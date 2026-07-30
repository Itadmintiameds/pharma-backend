package tiameds.pharmabackend.entity.master;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_device_specification_unit_master")
public class DeviceSpecificationUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_specification_unit_id")
    private Long deviceSpecificationUnitId;

    @Column(name = "device_specification_unit_name")
    private String deviceSpecificationUnitName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // FK → pharma_device_sub_category_master (sub-category)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_sub_category_id")
    @JsonIgnore
    private DeviceSubCategory deviceSubCategory;
}
