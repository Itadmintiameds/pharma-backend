package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistribution;

import java.util.List;

@Repository
public interface WarehouseDistributionRepository extends JpaRepository<WarehouseDistribution, Long> {

    // Latest allocation number for a given prefix (e.g. "ALLOC-2026-"), highest first.
    // Allocation numbers run as one global yearly sequence.
    @Query("""
        SELECT d.allocationNo
        FROM WarehouseDistribution d
        WHERE d.allocationNo LIKE CONCAT(:prefix, '%')
        ORDER BY d.allocationNo DESC
    """)
    List<String> findLatestAllocationNo(
            @Param("prefix") String prefix,
            Pageable pageable
    );
}
