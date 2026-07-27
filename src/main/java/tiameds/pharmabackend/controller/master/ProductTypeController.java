package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.ProductSubTypeDto;
import tiameds.pharmabackend.dto.master.ProductTypeDto;
import tiameds.pharmabackend.service.master.ProductSubTypeService;
import tiameds.pharmabackend.service.master.ProductTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/product-types")
@RequiredArgsConstructor
public class ProductTypeController {

    private final ProductTypeService productTypeService;
    private final ProductSubTypeService productSubTypeService;

    @GetMapping
    public ResponseEntity<List<ProductTypeDto>> getAllProductTypes(
            @RequestParam(required = false) Long productCategoryId) {

        if (productCategoryId != null) {
            return ResponseEntity.ok(productTypeService.getProductTypesByCategoryId(productCategoryId));
        }
        return ResponseEntity.ok(productTypeService.getAllProductTypes());
    }

    @GetMapping("/{productTypeId}")
    public ResponseEntity<ProductTypeDto> getProductTypeById(@PathVariable Long productTypeId) {
        return ResponseEntity.ok(productTypeService.getProductTypeById(productTypeId));
    }

    @GetMapping("/{productTypeId}/sub-types")
    public ResponseEntity<List<ProductSubTypeDto>> getProductSubTypesByTypeId(
            @PathVariable Long productTypeId) {

        return ResponseEntity.ok(productSubTypeService.getProductSubTypesByTypeId(productTypeId));
    }

    @PostMapping
    public ResponseEntity<ProductTypeDto> createProductType(@RequestBody ProductTypeDto productTypeDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productTypeService.createProductType(productTypeDto));
    }

    @PutMapping("/{productTypeId}")
    public ResponseEntity<ProductTypeDto> updateProductType(
            @PathVariable Long productTypeId,
            @RequestBody ProductTypeDto productTypeDto) {

        return ResponseEntity.ok(productTypeService.updateProductType(productTypeId, productTypeDto));
    }

    @PatchMapping("/{productTypeId}/status")
    public ResponseEntity<ProductTypeDto> updateProductTypeStatus(
            @PathVariable Long productTypeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                productTypeService.updateProductTypeStatus(productTypeId, request.getIsActive()));
    }
}
