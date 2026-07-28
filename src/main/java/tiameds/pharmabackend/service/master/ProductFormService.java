package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.ProductFormDto;

import java.util.List;

public interface ProductFormService {

    List<ProductFormDto> getAllProductForms();

    ProductFormDto getProductFormById(Long productFormId);

    List<ProductFormDto> getProductFormsByCategoryId(Long productCategoryId);

    ProductFormDto createProductForm(ProductFormDto productFormDto);

    ProductFormDto updateProductForm(Long productFormId, ProductFormDto productFormDto);

    ProductFormDto updateProductFormStatus(Long productFormId, Boolean isActive);
}
