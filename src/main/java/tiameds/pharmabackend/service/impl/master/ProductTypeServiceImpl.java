package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.ProductTypeDto;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.repository.master.ProductTypeRepository;
import tiameds.pharmabackend.service.master.ProductTypeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductTypeServiceImpl implements ProductTypeService {

    private final ProductTypeRepository productTypeRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductTypeDto> getAllProductTypes() {
        return productTypeRepository
                .findAll(Sort.by("productTypeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTypeDto getProductTypeById(Long productTypeId) {
        return toDto(findProductType(productTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductTypeDto> getProductTypesByCategoryId(Long productCategoryId) {

        if (!productCategoryRepository.existsById(productCategoryId)) {
            throw new RuntimeException("Product category not found with id: " + productCategoryId);
        }

        return productTypeRepository
                .findByProductCategory_ProductCategoryId(productCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductTypeDto createProductType(ProductTypeDto productTypeDto) {

        ProductType productType = new ProductType();
        productType.setProductTypeName(productTypeDto.getProductTypeName());

        if (productTypeDto.getProductCategoryId() != null) {
            productType.setProductCategory(findProductCategory(productTypeDto.getProductCategoryId()));
        }

        productType.setIsActive(productTypeDto.getIsActive() != null ? productTypeDto.getIsActive() : true);
        productType.setCreatedAt(LocalDateTime.now());

        return toDto(productTypeRepository.save(productType));
    }

    @Override
    public ProductTypeDto updateProductType(Long productTypeId, ProductTypeDto productTypeDto) {

        ProductType productType = findProductType(productTypeId);

        productType.setProductTypeName(productTypeDto.getProductTypeName());

        if (productTypeDto.getProductCategoryId() != null) {
            productType.setProductCategory(findProductCategory(productTypeDto.getProductCategoryId()));
        }

        if (productTypeDto.getIsActive() != null) {
            productType.setIsActive(productTypeDto.getIsActive());
        }
        productType.setModifiedAt(LocalDateTime.now());

        return toDto(productTypeRepository.save(productType));
    }

    @Override
    public ProductTypeDto updateProductTypeStatus(Long productTypeId, Boolean isActive) {

        ProductType productType = findProductType(productTypeId);

        productType.setIsActive(isActive);
        productType.setModifiedAt(LocalDateTime.now());

        return toDto(productTypeRepository.save(productType));
    }

    private ProductType findProductType(Long productTypeId) {
        return productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> new RuntimeException("Product type not found with id: " + productTypeId));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Product category not found with id: " + productCategoryId));
    }

    private ProductTypeDto toDto(ProductType productType) {
        ProductTypeDto dto = new ProductTypeDto();
        dto.setProductTypeId(productType.getProductTypeId());
        dto.setProductTypeName(productType.getProductTypeName());

        if (productType.getProductCategory() != null) {
            dto.setProductCategoryId(productType.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(productType.getProductCategory().getProductCategoryName());
        }

        dto.setIsActive(productType.getIsActive());
        return dto;
    }
}
