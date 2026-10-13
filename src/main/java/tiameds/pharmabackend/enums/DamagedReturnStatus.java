package tiameds.pharmabackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Whether the damaged quantity on a distribution line has been sent back to
 * the warehouse through a warehouse stock return.
 *
 * <p>Every line starts as Not Returned. It becomes Returned once a warehouse
 * return that points at the line is sent (Pending Receipt). Partially Returned
 * is reserved for returns that cover only part of the damaged quantity.
 *
 * <p>The label is what is stored in the database and sent/accepted in JSON.
 */
public enum DamagedReturnStatus {
    NOT_RETURNED("Not Returned"),
    RETURNED("Returned"),
    PARTIALLY_RETURNED("Partially Returned");

    // Value written by the distribution service before this enum existed.
    private static final String LEGACY_NOT_RETURNED = "Not return";

    private final String label;

    DamagedReturnStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // Accepts the label ("Not Returned") or the constant name ("NOT_RETURNED"),
    // ignoring case.
    @JsonCreator
    public static DamagedReturnStatus fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        if (LEGACY_NOT_RETURNED.equalsIgnoreCase(value.trim())) {
            return NOT_RETURNED;
        }

        for (DamagedReturnStatus status : values()) {
            if (status.label.equalsIgnoreCase(value.trim())
                    || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown damaged return status: " + value);
    }
}
