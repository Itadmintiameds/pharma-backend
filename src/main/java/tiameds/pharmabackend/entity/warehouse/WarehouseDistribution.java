package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.LocationType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pharma_warehouse_distribution",
        // Allocation numbers are sequenced per organization (each org starts from 1),
        // so the same number can recur across organizations — uniqueness is composite
        // (organization + allocation_no), not global on allocation_no alone.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wd_org_allocation_no",
                        columnNames = {"organization_id", "allocation_no"})
        },
        indexes = {
                @Index(name = "idx_wd_destination", columnList = "destination_type, destination_id"),
                @Index(name = "idx_wd_source", columnList = "source_type, source_id"),
                @Index(name = "idx_wd_requested_by", columnList = "allocation_requested_by")
        })
public class WarehouseDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_distribution_id")
    private Long warehouseDistributionId;

    // The organization that owns this allocation. Allocation numbers are sequenced
    // within it, so it also scopes the "next allocation number" lookup.
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "allocation_mode")       // Create Allocation By Myself or Against Stock Requirement
    private String allocationMode;

    // OLD: globally unique. Now unique only within an organization — see the
    // composite uk_wd_org_allocation_no constraint on the table above.
    // @Column(name = "allocation_no", nullable = false, unique = true)
    @Column(name = "allocation_no", nullable = false)
    private String allocationNo;

    @Column(name = "allocation_date")
    private LocalDateTime allocationDate;

    @Column(name = "distribution_type")     // Warehouse Distribution or Pharmacy Transfer
    private String distributionType;

    @Column(name = "reference")
    private String reference;

    @Column(name = "remarks")
    private String remarks;

    // OLD: free-text source type — replaced by LocationType enum to avoid magic strings
    // @Column(name = "source_type", nullable = false)       // warehouse or pharmacy
    // private String sourceType;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)       // WAREHOUSE or PHARMACY
    private LocationType sourceType;

    @Column(name = "source_id", nullable = false)     // warehouse_id or pharmacy_id
    private String sourceId;

    // OLD: free-text destination type — replaced by LocationType enum to avoid magic strings
    // @Column(name = "destination_type", nullable = false)      // warehouse or pharmacy
    // private String destinationType;
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false)      // WAREHOUSE or PHARMACY
    private LocationType destinationType;

    @Column(name = "destination_id", nullable = false)
    private String destinationId;

    @Column(name = "allocation_requested_by")   // requesting warehouse id
    private String allocationRequestedBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // Inverse side of the allocation lines. Read-only navigation for the detail view;
    // lines are still persisted explicitly via their own repository at create time.
    @OneToMany(mappedBy = "warehouseDistribution", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WarehouseDistributionDetails> details = new ArrayList<>();

    // Inverse side of the status history (one row appended per lifecycle transition),
    // ordered oldest-first. The PK tiebreaker keeps ordering deterministic when two
    // transitions share a timestamp, so the last element is always the current status.
    @OneToMany(mappedBy = "warehouseDistribution", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC, warehouseDistributionStatusId ASC")
    @JsonIgnore
    private List<WarehouseDistributionStatus> statuses = new ArrayList<>();

}
