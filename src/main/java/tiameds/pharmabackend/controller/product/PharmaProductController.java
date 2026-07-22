package tiameds.pharmabackend.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.product.PharmaProductDetailsDto;
import tiameds.pharmabackend.service.product.PharmaProductService;

@RestController
@RequestMapping("/product")
public class PharmaProductController {

    @Autowired
    private PharmaProductService pharmaProductService;

    @PostMapping("/onboard")
    public ResponseEntity<String> onboardProduct(@RequestBody PharmaProductDetailsDto dto) {
        String productId = pharmaProductService.onboardProduct(dto);
        return ResponseEntity.ok("Product successfully onboarded with ID: " + productId);
    }
}
