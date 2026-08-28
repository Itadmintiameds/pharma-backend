package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.repository.warehouse.WarehouseDistributionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates the next allocation number: one yearly sequence PER ORGANIZATION,
 * pattern {@code ALC-<year>-00001} (mirrors billing's bill-number scheme). The
 * number format is unchanged from the global scheme — the per-organization
 * scoping comes from filtering the "latest number" lookup to the organization,
 * so every organization starts from 1 (and resets each year). Because the same
 * number can therefore recur across organizations, uniqueness is enforced on
 * {@code (organization_id, allocation_no)} rather than {@code allocation_no} alone.
 * <p>
 * This is a "next value" preview — it is not reserved. The authoritative number
 * is assigned inside the create transaction; two concurrent creates in the same
 * organization can compute the same value, in which case the composite unique
 * constraint rejects the loser (which should retry).
 */
@Component
@RequiredArgsConstructor
public class AllocationNoGenerator {

    private final WarehouseDistributionRepository distributionRepository;

    /**
     * Next allocation number for {@code organizationId}. The {@code ALC-<year>-}
     * prefix bounds the search to the current year, and {@code organizationId}
     * scopes it to this organization, so each organization has its own sequence
     * starting at 1.
     */
    public String generate(Long organizationId) {
        int year = LocalDate.now().getYear();
        String prefix = "ALC-" + year + "-";

        List<String> latest = distributionRepository.findLatestAllocationNo(
                organizationId, prefix, PageRequest.of(0, 1));

        int nextNumber = 1;
        if (!latest.isEmpty()) {
            String numberPart = latest.get(0).substring(prefix.length());
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }
}
