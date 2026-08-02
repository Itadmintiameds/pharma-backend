package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.ProductCategoryDto;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.service.master.ProductCategoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryDto> getAllProductCategories() {
        return productCategoryRepository
                .findAll(Sort.by("productCategoryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryDto getProductCategoryById(Long productCategoryId) {
        return toDto(findProductCategory(productCategoryId));
    }

    @Override
    public ProductCategoryDto createProductCategory(ProductCategoryDto productCategoryDto) {

        ProductCategory productCategory = new ProductCategory();
        productCategory.setProductCategoryName(productCategoryDto.getProductCategoryName());
        productCategory.setIsActive(productCategoryDto.getIsActive() != null ? productCategoryDto.getIsActive() : true);
        productCategory.setCreatedAt(LocalDateTime.now());

        return toDto(productCategoryRepository.save(productCategory));
    }

    @Override
    public ProductCategoryDto updateProductCategory(Long productCategoryId, ProductCategoryDto productCategoryDto) {

        ProductCategory productCategory = findProductCategory(productCategoryId);

        productCategory.setProductCategoryName(productCategoryDto.getProductCategoryName());
        if (productCategoryDto.getIsActive() != null) {
            productCategory.setIsActive(productCategoryDto.getIsActive());
        }
        productCategory.setModifiedAt(LocalDateTime.now());

        return toDto(productCategoryRepository.save(productCategory));
    }

    @Override
    public ProductCategoryDto updateProductCategoryStatus(Long productCategoryId, Boolean isActive) {

        ProductCategory productCategory = findProductCategory(productCategoryId);

        productCategory.setIsActive(isActive);
        productCategory.setModifiedAt(LocalDateTime.now());

        return toDto(productCategoryRepository.save(productCategory));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + productCategoryId));
    }

    private ProductCategoryDto toDto(ProductCategory productCategory) {
        ProductCategoryDto dto = new ProductCategoryDto();
        dto.setProductCategoryId(productCategory.getProductCategoryId());
        dto.setProductCategoryName(productCategory.getProductCategoryName());
        dto.setIsActive(productCategory.getIsActive());
        return dto;
    }
}
