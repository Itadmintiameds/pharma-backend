package tiameds.pharmabackend.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.product.ProductDetailResponseDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.ProductStockSummaryDto;
import tiameds.pharmabackend.service.product.ProductService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String productId) {
        pharmaProductService.deleteProduct(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product successfully deleted");
        return ResponseEntity.ok(response);
    }
}
