package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.ProductCategoryDto;
import tiameds.pharmabackend.service.master.ProductCategoryService;

import java.util.List;

@RestController
@RequestMapping("/master/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public ResponseEntity<List<ProductCategoryDto>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllProductCategories());
    }

    @GetMapping("/{productCategoryId}")
    public ResponseEntity<ProductCategoryDto> getProductCategoryById(@PathVariable Long productCategoryId) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(productCategoryId));
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDto> createProductCategory(
            @RequestBody ProductCategoryDto productCategoryDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productCategoryService.createProductCategory(productCategoryDto));
    }

    @PutMapping("/{productCategoryId}")
    public ResponseEntity<ProductCategoryDto> updateProductCategory(
            @PathVariable Long productCategoryId,
            @RequestBody ProductCategoryDto productCategoryDto) {

        return ResponseEntity.ok(
                productCategoryService.updateProductCategory(productCategoryId, productCategoryDto));
    }

    @PatchMapping("/{productCategoryId}/status")
    public ResponseEntity<ProductCategoryDto> updateProductCategoryStatus(
            @PathVariable Long productCategoryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                productCategoryService.updateProductCategoryStatus(productCategoryId, request.getIsActive()));
    }
}
