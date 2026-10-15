package tiameds.pharmabackend.controller.admin;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.security.OperatorPasscode;
import tiameds.pharmabackend.service.admin.OrganizationDirectoryService;

/**
 * Directory of every organization with its pharmacies and warehouses (ids and names),
 * so an operator can find the pharmacyId / warehouseId to pass to /product/bulk-upload.
 * <p>
 * Open in SecurityConfig (no JWT) and gated by the same hardcoded passcode as the bulk
 * upload, sent in the X-Upload-Passcode header. Spans all tenants by design.
 */
@RestController
@RequestMapping("/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationDirectoryController {

    private final OrganizationDirectoryService organizationDirectoryService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = OperatorPasscode.HEADER, required = false) String passcode,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (!OperatorPasscode.isValid(passcode)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or missing " + OperatorPasscode.HEADER));
        }

        return ResponseEntity.ok(organizationDirectoryService.search(search, page, size));
    }
}
