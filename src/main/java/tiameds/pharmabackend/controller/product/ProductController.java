package tiameds.pharmabackend.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.product.*;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.product.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService pharmaProductService;

    @PostMapping("/onboard")
    public ResponseEntity<Map<String, Object>> onboardProduct(@RequestBody ProductDetailsDto dto) {
        ProductDetailsDto savedDto = pharmaProductService.onboardProduct(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product successfully onboarded with ID: " + savedDto.getProductId());
        response.put("data", savedDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        java.util.List<ProductDetailsDto> products = pharmaProductService.getAllProducts();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Products retrieved successfully");
        response.put("count", products.size());
        response.put("data", products);
        return ResponseEntity.ok(response);
    }

    // API 1: all products of the current pharmacy with stock + expiry status
    @GetMapping("/stock-summary")
    public ResponseEntity<Map<String, Object>> getProductStockSummaries() {
        List<ProductStockSummaryDto> summaries = pharmaProductService.getProductStockSummaries();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product stock summaries retrieved successfully");
        response.put("count", summaries.size());
        response.put("data", summaries);
        return ResponseEntity.ok(response);
    }

    // API 1 (org catalog): all products of the caller's organization (derived from the
    // authenticated user), each with the stock held at the caller's current location
    // (pharmacy or warehouse, from the header), ordered by stock descending. Products
    // with no stock at that location still appear (stock 0).
    @GetMapping("/stock-summary/organization")
    public ResponseEntity<Map<String, Object>> getProductStockSummariesByOrganization() {
        List<ProductStockSummaryDto> summaries =
                pharmaProductService.getProductStockSummariesByOrganization();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product stock summaries retrieved successfully");
        response.put("count", summaries.size());
        response.put("data", summaries);
        return ResponseEntity.ok(response);
    }

    // Pre-onboard duplicate check for the frontend: is there already a product with the
    // same name, brand and HSN in the caller's organization? (org derived from the user)
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> productExists(
            @RequestParam String productName,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) String hsnNo) {
        boolean exists = pharmaProductService
                .productExistsForOrganization(productName, brandName, hsnNo);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists
                ? "A product with the same name, brand and HSN already exists for this organization"
                : "No matching product found");
        return ResponseEntity.ok(response);
    }

    // Dashboard KPI: product counts bucketed by nearest in-stock expiry
    @GetMapping("/expiry-kpi")
    public ResponseEntity<Map<String, Object>> getExpiryKpi() {
        ProductExpiryKpiDto kpi = pharmaProductService.getExpiryKpi();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Expiry KPI retrieved successfully");
        response.put("data", kpi);
        return ResponseEntity.ok(response);
    }

    // Dashboard KPI: in-stock batch counts bucketed independently by each batch's expiry
    @GetMapping("/batch-expiry-kpi")
    public ResponseEntity<Map<String, Object>> getBatchExpiryKpi() {
        BatchExpiryKpiDto kpi = pharmaProductService.getBatchExpiryKpi();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batch expiry KPI retrieved successfully");
        response.put("data", kpi);
        return ResponseEntity.ok(response);
    }

    // All batches of the pharmacy with product, packaging, stock and pricing
    @GetMapping("/batches")
    public ResponseEntity<Map<String, Object>> getAllBatches() {
        List<BatchStockDto> batches = pharmaProductService.getAllBatches();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batches retrieved successfully");
        response.put("count", batches.size());
        response.put("data", batches);
        return ResponseEntity.ok(response);
    }

    // Batches available at a specific pharmacy (must belong to the caller's
    // organization) — used when the pharmacy being picked from (e.g. the source of a
    // pharmacy-to-pharmacy transfer) differs from the caller's currently active pharmacy.
    @GetMapping("/batches/pharmacy/{pharmacyId}")
    public ResponseEntity<Map<String, Object>> getBatchesForPharmacy(@PathVariable String pharmacyId) {
        List<BatchStockDto> batches = pharmaProductService.getBatchesForPharmacy(pharmacyId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batches retrieved successfully");
        response.put("count", batches.size());
        response.put("data", batches);
        return ResponseEntity.ok(response);
    }

    // One batch with the same detail as the listing
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatchById(@PathVariable String batchId) {
        BatchStockDto batch = pharmaProductService.getBatchById(batchId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batch retrieved successfully");
        response.put("data", batch);
        return ResponseEntity.ok(response);
    }

    // API 2: complete details of one product with batches grouped per package
    @GetMapping("/{productId}/details")
    public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable String productId) {
        ProductDetailResponseDto product = pharmaProductService.getProductDetails(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product details retrieved successfully");
        response.put("data", product);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable String productId) {
        ProductDetailsDto product = pharmaProductService.getProductById(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product retrieved successfully");
        response.put("data", product);
        return ResponseEntity.ok(response);
    }

    // Add a new package (optionally with batches) to an existing product
    @PostMapping("/{productId}/package")
    public ResponseEntity<Map<String, Object>> addPackage(
            @PathVariable String productId,
            @RequestBody AddPackageRequest request) {
        ProductDetailResponseDto product = pharmaProductService.addPackage(productId, request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Package added successfully");
        response.put("data", product);
        return ResponseEntity.ok(response);
    }

    // Add one or more batches to existing packages of a product
    @PostMapping("/{productId}/batch")
    public ResponseEntity<Map<String, Object>> addBatches(
            @PathVariable String productId,
            @RequestBody List<BatchDetailsDto> batches) {
        ProductDetailResponseDto product = pharmaProductService.addBatches(productId, batches);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batches added successfully");
        response.put("data", product);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String productId) {
        pharmaProductService.deleteProduct(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product successfully deleted");
        return ResponseEntity.ok(response);
    }


    @GetMapping("/batchExists")
    public ResponseEntity<?> checkBatchNumberExists(
            @RequestParam String batchNumber,
            @RequestParam String productId,
            @RequestParam String packagingId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean exists = pharmaProductService.existsByBatchNumber(
                currentUser.getUser(),
                batchNumber,
                productId,
                packagingId
        );

        return ResponseEntity.ok(exists);
    }
}
