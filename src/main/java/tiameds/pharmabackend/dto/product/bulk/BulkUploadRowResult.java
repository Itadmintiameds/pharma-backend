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
     * Stock written into inventory for this line, in smallest units.
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
         * The product already existed; this line added a batch (and possibly a package) to it.
         */
        UPDATED,
        /**
         * Nothing to do — the batch is already on record. Stock is deliberately not
         * re-added, so re-uploading the same file does not inflate inventory.
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
