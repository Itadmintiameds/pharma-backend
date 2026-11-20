package tiameds.pharmabackend.dto.product.bulk;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of one CSV line, so the operator can fix the file line by line
 * instead of being told only that "the upload failed".
 */
@Data
public class BulkUploadRowResult {

    /**
     * Line number in the uploaded file as a spreadsheet shows it (header is line 1).
     */
    private int line;

    private String productCode;
    private String productName;
    private String batchNumber;

    private Status status;

    /**
     * Ids of what this line produced — null when the line failed.
     */
    private String productId;
    private String packagingId;
    private String batchId;

    /**
     * True when this line created a new batch; false when it reused one already on
     * record for the organization (another location may have imported it first).
     */
    private boolean batchCreated;

    /**
     * This location's stock for the batch before the line was applied, in smallest
     * units. Null when the location had no stock row for the batch.
     */
    private Long previousStock;

    /**
     * Change this line made to this location's stock, in smallest units. Negative when
     * the file corrected the stock downwards.
     */
    private Long stockAdded;

    /**
     * Why the line failed, or why it was skipped.
     */
    private String error;

    /**
     * Data that was accepted but not stored — an unknown master name, or a column
     * that has no home on the resolved category. The line still succeeded.
     */
    private List<String> warnings = new ArrayList<>();

    public enum Status {
        /**
         * A new product was created from this line.
         */
        CREATED,
        /**
         * The product already existed; this line added a batch (and possibly a package)
         * to it, or set this location's stock for a batch already on record to the
         * file's value (see {@link #batchCreated} and {@link #previousStock}).
         */
        UPDATED,
        /**
         * Nothing to do — the batch is on record and this location's stock already
         * matches the file (or the file gives no stock for it).
         */
        SKIPPED,
        /**
         * The line could not be processed; see {@link #error}.
         */
        FAILED
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }
}
