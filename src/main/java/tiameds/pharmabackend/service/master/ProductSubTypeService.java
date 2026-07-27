package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.ProductSubTypeDto;

import java.util.List;

public interface ProductSubTypeService {

    List<ProductSubTypeDto> getAllProductSubTypes();

    ProductSubTypeDto getProductSubTypeById(Long productSubTypeId);

    List<ProductSubTypeDto> getProductSubTypesByTypeId(Long productTypeId);

    ProductSubTypeDto createProductSubType(ProductSubTypeDto productSubTypeDto);

    ProductSubTypeDto updateProductSubType(Long productSubTypeId, ProductSubTypeDto productSubTypeDto);

    ProductSubTypeDto updateProductSubTypeStatus(Long productSubTypeId, Boolean isActive);
}
