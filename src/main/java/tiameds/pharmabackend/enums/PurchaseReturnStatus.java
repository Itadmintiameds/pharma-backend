package tiameds.pharmabackend.enums;

/**
 * Lifecycle of a purchase return.
 *
 * <p>The distinction that matters is DRAFT vs CONFIRMED: a draft is a saved
 * document and nothing more — no stock leaves, no audit row is written, so it
 * can be edited freely. Confirming it is the point the goods are treated as
 * gone back to the supplier, and that is when inventory moves. A return can
 * only make that trip once.
 */
public enum PurchaseReturnStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED
}
