package tiameds.pharmabackend.service.product.bulk;

import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.product.bulk.BulkUploadResponse;

public interface ProductBulkUploadService {

    /**
     * Imports a bulk-upload CSV into one pharmacy or one warehouse.
     * <p>
     * The destination is taken from {@code warehouseId} or {@code pharmacyId} when
     * either is given; otherwise it falls back to the location the caller is already
     * operating on (the {@code X-Pharmacy-Id} / {@code X-Warehouse-Id} headers), which
     * needs a logged-in user. A call authenticated with the internal API key has no
     * user, so it must name the destination explicitly.
     *
     * @param dryRun validate and report without writing anything
     */
    BulkUploadResponse upload(MultipartFile file, boolean dryRun, String pharmacyId, String warehouseId);

    /**
     * The empty template — the header row the upload expects.
     */
    String templateCsv();
}
