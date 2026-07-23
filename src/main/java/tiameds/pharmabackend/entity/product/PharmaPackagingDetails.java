package tiameds.pharmabackend.entity.product;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_packaging_details")
public class PharmaPackagingDetails {

    @Id
    @Column(name = "packaging_id", length = 30)
    private String packagingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private PharmaProductDetails product;

    @Column(name = "purchase_unit", length = 50)
    private String purchaseUnit;

    @Column(name = "purchase_unit_contains")
    private Long purchaseUnitContains;

    // @Column(name = "secondary_unit", length = 50)
    // private String secondaryUnit;

    // @Column(name = "secondary_unit_contains")
    // private Long secondaryUnitContains;

    @Column(name = "smallest_unit", length = 50)
    private String smallestUnit;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
