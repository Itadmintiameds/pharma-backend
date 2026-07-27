package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.ProductFormDto;
import tiameds.pharmabackend.service.master.ProductFormService;

import java.util.List;

@RestController
@RequestMapping("/master/product-forms")
@RequiredArgsConstructor
public class ProductFormController {

    private final ProductFormService productFormService;

    @GetMapping
    public ResponseEntity<List<ProductFormDto>> getAllProductForms() {
        return ResponseEntity.ok(productFormService.getAllProductForms());
    }

    @GetMapping("/{productFormId}")
    public ResponseEntity<ProductFormDto> getProductFormById(@PathVariable Long productFormId) {
        return ResponseEntity.ok(productFormService.getProductFormById(productFormId));
    }

    @PostMapping
    public ResponseEntity<ProductFormDto> createProductForm(@RequestBody ProductFormDto productFormDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productFormService.createProductForm(productFormDto));
    }

    @PutMapping("/{productFormId}")
    public ResponseEntity<ProductFormDto> updateProductForm(
            @PathVariable Long productFormId,
            @RequestBody ProductFormDto productFormDto) {

        return ResponseEntity.ok(productFormService.updateProductForm(productFormId, productFormDto));
    }

    @PatchMapping("/{productFormId}/status")
    public ResponseEntity<ProductFormDto> updateProductFormStatus(
            @PathVariable Long productFormId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                productFormService.updateProductFormStatus(productFormId, request.getIsActive()));
    }
}
