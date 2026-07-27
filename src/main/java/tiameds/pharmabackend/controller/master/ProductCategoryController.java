package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.NetQuantityUnitDto;
import tiameds.pharmabackend.dto.master.ProductCategoryDto;
import tiameds.pharmabackend.dto.master.ProductTypeDto;
import tiameds.pharmabackend.service.master.NetQuantityUnitService;
import tiameds.pharmabackend.service.master.ProductCategoryService;
import tiameds.pharmabackend.service.master.ProductTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final ProductTypeService productTypeService;
    private final NetQuantityUnitService netQuantityUnitService;

    @GetMapping
    public ResponseEntity<List<ProductCategoryDto>> getAllProductCategories() {
        return ResponseEntity.ok(productCategoryService.getAllProductCategories());
    }

    @GetMapping("/{productCategoryId}")
    public ResponseEntity<ProductCategoryDto> getProductCategoryById(@PathVariable Long productCategoryId) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(productCategoryId));
    }

    @GetMapping("/{productCategoryId}/product-types")
    public ResponseEntity<List<ProductTypeDto>> getProductTypesByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(productTypeService.getProductTypesByCategoryId(productCategoryId));
    }

    @GetMapping("/{productCategoryId}/net-quantity-units")
    public ResponseEntity<List<NetQuantityUnitDto>> getNetQuantityUnitsByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(netQuantityUnitService.getNetQuantityUnitsByCategoryId(productCategoryId));
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
