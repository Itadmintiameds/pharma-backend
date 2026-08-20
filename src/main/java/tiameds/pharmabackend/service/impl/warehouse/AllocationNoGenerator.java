package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.repository.warehouse.WarehouseDistributionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates the next allocation number: one global yearly sequence,
 * pattern {@code ALC-<year>-00001} (mirrors billing's bill-number scheme).
 * <p>
 * This is a "next value" preview — it is not reserved. The authoritative number
 * is assigned inside the create transaction; two concurrent creates can compute
 * the same value, in which case the unique constraint on allocation_no rejects
 * the loser (which should retry).
 */
@Component
@RequiredArgsConstructor
public class AllocationNoGenerator {

    private final WarehouseDistributionRepository distributionRepository;

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "ALC-" + year + "-";

        List<String> latest = distributionRepository.findLatestAllocationNo(
                prefix, PageRequest.of(0, 1));

        int nextNumber = 1;
        if (!latest.isEmpty()) {
            String numberPart = latest.get(0).substring(prefix.length());
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }
}
