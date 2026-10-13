package tiameds.pharmabackend.enums;

/**
 * How much of a purchase — or of a single purchase line — has been sent back to
 * the supplier. Shared by Purchase and PurchaseDetails so the header and its
 * lines speak the same language: the header is NOT_RETURNED while every line is,
 * FULLY_RETURNED only once every line is, and PARTIALLY_RETURNED in between.
 *
 * <p>Every purchase starts at NOT_RETURNED; the value is recalculated when a
 * purchase return is created.
 */
public enum ReturnStatus {
    NOT_RETURNED,
    PARTIALLY_RETURNED,
    FULLY_RETURNED
}
