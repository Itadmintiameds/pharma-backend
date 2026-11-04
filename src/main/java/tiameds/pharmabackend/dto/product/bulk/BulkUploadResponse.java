package tiameds.pharmabackend.dto.product.bulk;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of one bulk upload: a tally plus the per-line detail.
 */
@Data
public class BulkUploadResponse {

    /**
     * Where the upload landed — the pharmacy or warehouse resolved from the request.
     */
    private String locationType;
    private String locationId;
    private String locationName;

    /**
     * True when the file was only validated and nothing was written.
     */
    private boolean dryRun;

    private int totalRows;
    private int productsCreated;
    private int productsUpdated;
    private int batchesCreated;
    private int rowsSkipped;
    private int rowsFailed;

    /**
     * Total stock written into inventory, in smallest units.
     */
    private long stockAdded;

    private List<BulkUploadRowResult> rows = new ArrayList<>();

    /**
     * Problems with the file as a whole (a missing column, an unreadable file)
     * rather than with any one line.
     */
    private List<String> fileErrors = new ArrayList<>();
}
