package tiameds.pharmabackend.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.product.*;
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

    // Dashboard KPI: product counts bucketed by nearest in-stock expiry
    @GetMapping("/expiry-kpi")
    public ResponseEntity<Map<String, Object>> getExpiryKpi() {
        ProductExpiryKpiDto kpi = pharmaProductService.getExpiryKpi();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Expiry KPI retrieved successfully");
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
}
