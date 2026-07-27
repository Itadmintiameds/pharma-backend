package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.ProductTypeDto;

import java.util.List;

public interface ProductTypeService {

    List<ProductTypeDto> getAllProductTypes();

    ProductTypeDto getProductTypeById(Long productTypeId);

    List<ProductTypeDto> getProductTypesByCategoryId(Long productCategoryId);

    ProductTypeDto createProductType(ProductTypeDto productTypeDto);

    ProductTypeDto updateProductType(Long productTypeId, ProductTypeDto productTypeDto);

    ProductTypeDto updateProductTypeStatus(Long productTypeId, Boolean isActive);
}
