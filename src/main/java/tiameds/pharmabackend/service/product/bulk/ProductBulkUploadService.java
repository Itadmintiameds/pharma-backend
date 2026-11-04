package tiameds.pharmabackend.service.product.bulk;

import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.product.bulk.BulkUploadResponse;

public interface ProductBulkUploadService {

    /**
     * Imports a bulk-upload CSV into one pharmacy or one warehouse.
     * <p>
     * The destination is taken only from {@code warehouseId} or {@code pharmacyId}
     * (warehouse wins if both are given); one of them is required. The caller is
     * another service holding the operator passcode, not a logged-in user, so no
     * header or user context is consulted.
     *
     * @param dryRun validate and report without writing anything
     */
    BulkUploadResponse upload(MultipartFile file, boolean dryRun, String pharmacyId, String warehouseId);

    /**
     * The empty template — the header row the upload expects.
     */
    String templateCsv();
}
