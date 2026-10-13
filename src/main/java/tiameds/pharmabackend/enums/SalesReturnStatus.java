package tiameds.pharmabackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a sales return document itself (not of the bill it returns
 * against — that is {@link BillReturnStatus}).
 *
 * <p>Every return is Completed on create: the stock goes back immediately.
 * A Draft state is expected later; it is stored as a label through a
 * converter so adding it needs no database constraint change.
 */
public enum SalesReturnStatus {
    COMPLETED("Completed");

    private final String label;

    SalesReturnStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    // Accepts the label ("Completed") or the constant name ("COMPLETED"),
    // ignoring case.
    @JsonCreator
    public static SalesReturnStatus fromValue(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        for (SalesReturnStatus status : values()) {
            if (status.label.equalsIgnoreCase(value.trim())
                    || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown sales return status: " + value);
    }
}
