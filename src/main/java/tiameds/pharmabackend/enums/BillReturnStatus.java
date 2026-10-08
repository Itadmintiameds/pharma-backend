package tiameds.pharmabackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How much of a bill has come back through sales returns.
 *
 * <p>Every bill starts as Not Returned. After each sales return it is
 * re-derived from the bill's total quantity against the total quantity
 * returned so far: some returned is Partially Returned, all of it is Returned.
 *
 * <p>The label is what is stored in the database and sent/accepted in JSON.
 */
public enum BillReturnStatus {
    NOT_RETURNED("Not Returned"),
    PARTIALLY_RETURNED("Partially Returned"),
    RETURNED("Returned");

    private final String label;

    BillReturnStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // Accepts the label ("Not Returned") or the constant name ("NOT_RETURNED"),
    // ignoring case.
    @JsonCreator
    public static BillReturnStatus fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        for (BillReturnStatus status : values()) {
            if (status.label.equalsIgnoreCase(value.trim())
                    || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown bill return status: " + value);
    }
}
