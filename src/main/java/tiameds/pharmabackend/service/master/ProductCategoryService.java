package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.ProductCategoryDto;

import java.util.List;

public interface ProductCategoryService {

    List<ProductCategoryDto> getAllProductCategories();

    ProductCategoryDto getProductCategoryById(Long productCategoryId);

    ProductCategoryDto createProductCategory(ProductCategoryDto productCategoryDto);

    ProductCategoryDto updateProductCategory(Long productCategoryId, ProductCategoryDto productCategoryDto);

    ProductCategoryDto updateProductCategoryStatus(Long productCategoryId, Boolean isActive);
}
