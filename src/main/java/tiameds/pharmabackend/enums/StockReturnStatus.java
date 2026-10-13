package tiameds.pharmabackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle of a stock return from a pharmacy back to a warehouse.
 *
 * <p>Draft is a saved document only. Pending Receipt means the pharmacy has
 * dispatched the goods and the warehouse has yet to receive them. Complete is
 * set once the warehouse has recorded what it received.
 *
 * <p>The label is what is stored in the database and sent/accepted in JSON.
 */
public enum StockReturnStatus {
    DRAFT("Draft"),
    PENDING_RECEIPT("Pending Receipt"),
    COMPLETE("Complete");

    private final String label;

    StockReturnStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // Accepts the label ("Pending Receipt") or the constant name ("PENDING_RECEIPT"),
    // ignoring case.
    @JsonCreator
    public static StockReturnStatus fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        for (StockReturnStatus status : values()) {
            if (status.label.equalsIgnoreCase(value.trim())
                    || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown stock return status: " + value);
    }
}
