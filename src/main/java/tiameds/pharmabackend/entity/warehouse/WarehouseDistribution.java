package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.PharmacyDetails;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pharma_warehouse_distribution")
public class WarehouseDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_distribution_id")
    private Long warehouseDistributionId;

    @Column(name = "allocation_mode")       // Create Allocation By Myself or Against Stock Requirement
    private String allocationMode;

    @Column(name = "allocation_no")
    private String allocationNo;

    @Column(name = "allocation_date")
    private LocalDateTime allocationDate;

    @Column(name = "distribution_type")     // Warehouse Distribution or Pharmacy Transfer
    private String distributionType;

    @Column(name = "reference")
    private String reference;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "source_type")       // warehouse or pharmacy
    private String sourceType;

    @Column(name = "source_id")     // warehouse_id or pharmacy_id
    private String sourceId;

    @Column(name = "destination_type")      // warehouse or pharmacy
    private String destinationType;

    @Column(name = "destination_id")
    private String destinationId;

    @Column(name = "requested_by")      // requested warehouse Id
    private String requestedBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

}
