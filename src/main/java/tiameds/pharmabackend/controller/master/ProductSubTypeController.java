package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.ProductSubTypeDto;
import tiameds.pharmabackend.service.master.ProductSubTypeService;

import java.util.List;

@RestController
@RequestMapping("/master/product-sub-types")
@RequiredArgsConstructor
public class ProductSubTypeController {

    private final ProductSubTypeService productSubTypeService;

    @GetMapping
    public ResponseEntity<List<ProductSubTypeDto>> getAllProductSubTypes(
            @RequestParam(required = false) Long productTypeId) {

        if (productTypeId != null) {
            return ResponseEntity.ok(productSubTypeService.getProductSubTypesByTypeId(productTypeId));
        }
        return ResponseEntity.ok(productSubTypeService.getAllProductSubTypes());
    }

    @GetMapping("/{productSubTypeId}")
    public ResponseEntity<ProductSubTypeDto> getProductSubTypeById(@PathVariable Long productSubTypeId) {
        return ResponseEntity.ok(productSubTypeService.getProductSubTypeById(productSubTypeId));
    }

    @PostMapping
    public ResponseEntity<ProductSubTypeDto> createProductSubType(
            @RequestBody ProductSubTypeDto productSubTypeDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productSubTypeService.createProductSubType(productSubTypeDto));
    }

    @PutMapping("/{productSubTypeId}")
    public ResponseEntity<ProductSubTypeDto> updateProductSubType(
            @PathVariable Long productSubTypeId,
            @RequestBody ProductSubTypeDto productSubTypeDto) {

        return ResponseEntity.ok(
                productSubTypeService.updateProductSubType(productSubTypeId, productSubTypeDto));
    }

    @PatchMapping("/{productSubTypeId}/status")
    public ResponseEntity<ProductSubTypeDto> updateProductSubTypeStatus(
            @PathVariable Long productSubTypeId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                productSubTypeService.updateProductSubTypeStatus(productSubTypeId, request.getIsActive()));
    }
}
