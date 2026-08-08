package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.DeviceCategoryDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.MaterialTypeDto;
import tiameds.pharmabackend.dto.master.NetQuantityUnitDto;
import tiameds.pharmabackend.dto.master.ProductCategoryDto;
import tiameds.pharmabackend.dto.master.ProductFormDto;
import tiameds.pharmabackend.dto.master.ProductTypeDto;
import tiameds.pharmabackend.dto.master.PurchaseSmallestUnitDto;
import tiameds.pharmabackend.service.master.DeviceCategoryService;
import tiameds.pharmabackend.service.master.MaterialTypeService;
import tiameds.pharmabackend.service.master.NetQuantityUnitService;
import tiameds.pharmabackend.service.master.ProductCategoryService;
import tiameds.pharmabackend.service.master.ProductFormService;
import tiameds.pharmabackend.service.master.ProductTypeService;
import tiameds.pharmabackend.service.master.PurchaseSmallestUnitService;

import java.util.List;

@RestController
@RequestMapping("/master/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;
    private final ProductTypeService productTypeService;
    private final NetQuantityUnitService netQuantityUnitService;
    private final ProductFormService productFormService;
    private final DeviceCategoryService deviceCategoryService;
    private final MaterialTypeService materialTypeService;
    private final PurchaseSmallestUnitService purchaseSmallestUnitService;

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

    @GetMapping("/{productCategoryId}/product-forms")
    public ResponseEntity<List<ProductFormDto>> getProductFormsByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(productFormService.getProductFormsByCategoryId(productCategoryId));
    }

    @GetMapping("/{productCategoryId}/device-categories")
    public ResponseEntity<List<DeviceCategoryDto>> getDeviceCategoriesByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(deviceCategoryService.getDeviceCategoriesByCategoryId(productCategoryId));
    }

    @GetMapping("/{productCategoryId}/material-types")
    public ResponseEntity<List<MaterialTypeDto>> getMaterialTypesByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(materialTypeService.getMaterialTypesByCategoryId(productCategoryId));
    }

    @GetMapping("/{productCategoryId}/purchase-smallest-units")
    public ResponseEntity<List<PurchaseSmallestUnitDto>> getPurchaseSmallestUnitsByCategoryId(
            @PathVariable Long productCategoryId) {

        return ResponseEntity.ok(purchaseSmallestUnitService.getPurchaseSmallestUnitsByCategoryId(productCategoryId));
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
