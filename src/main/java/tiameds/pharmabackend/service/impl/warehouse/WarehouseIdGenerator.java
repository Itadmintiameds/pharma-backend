package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;

/**
 * Generates the manually-assigned String primary key for a Warehouse.
 * Pattern: org prefix + name prefix + "WH" + 4-digit sequence
 * (e.g. organization "Tiameds", warehouse "Central Store" -> TIACEWH0001).
 */
@Component
@RequiredArgsConstructor
public class WarehouseIdGenerator {

    private final WarehouseRepository warehouseRepository;

    public String generate(String organizationName, String warehouseName) {

        String orgPrefix = letterPrefix(organizationName, 3);
        String namePrefix = letterPrefix(warehouseName, 2);

        String prefix = orgPrefix + namePrefix + "WH";

        String latestId = warehouseRepository.findLatestWarehouseId();

        int nextSequence = 1;

        if (latestId != null && !latestId.isBlank()) {

            String numericPart = latestId.replaceAll("[^0-9]", "");

            nextSequence = Integer.parseInt(numericPart) + 1;
        }

        return prefix + String.format("%04d", nextSequence);
    }

    /**
     * Uppercase letters-only prefix of the given length, drawn from the source text.
     * Falls back to "X" padding when the source has too few letters.
     */
    private String letterPrefix(String source, int length) {

        String letters = (source == null) ? "" : source.replaceAll("[^A-Za-z]", "").toUpperCase();

        StringBuilder prefix = new StringBuilder(
                letters.substring(0, Math.min(length, letters.length())));

        while (prefix.length() < length) {
            prefix.append('X');
        }

        return prefix.toString();
    }
}
