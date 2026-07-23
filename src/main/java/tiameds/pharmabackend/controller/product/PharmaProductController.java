package tiameds.pharmabackend.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.product.PharmaProductDetailsDto;
import tiameds.pharmabackend.service.product.PharmaProductService;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/product")
public class PharmaProductController {

    @Autowired
    private PharmaProductService pharmaProductService;

    @PostMapping("/onboard")
    public ResponseEntity<Map<String, Object>> onboardProduct(@RequestBody PharmaProductDetailsDto dto) {
        PharmaProductDetailsDto savedDto = pharmaProductService.onboardProduct(dto);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product successfully onboarded with ID: " + savedDto.getProductId());
        response.put("data", savedDto);
        
        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        java.util.List<PharmaProductDetailsDto> products = pharmaProductService.getAllProducts();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Products retrieved successfully");
        response.put("count", products.size());
        response.put("data", products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable String productId) {
        PharmaProductDetailsDto product = pharmaProductService.getProductById(productId);
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
