package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pharma_warehouse_distribution_status")
public class WarehouseDistributionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_distribution_status_id")
    private Long warehouseDistributionStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_distribution_id")
    @JsonIgnore
    private WarehouseDistribution warehouseDistribution;

    @Column(name = "warehouse_distribution_status")
    // Allocation Created, Products Dispatched, Pending Receipt, Stock Received, Stock Rejected etc
    private String warehouseDistributionStatus;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
