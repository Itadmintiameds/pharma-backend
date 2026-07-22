package tiameds.pharmabackend.service.product;

import tiameds.pharmabackend.dto.product.PharmaProductDetailsDto;

public interface PharmaProductService {
    PharmaProductDetailsDto onboardProduct(PharmaProductDetailsDto dto);
    java.util.List<PharmaProductDetailsDto> getAllProducts();
    PharmaProductDetailsDto getProductById(String productId);
    void deleteProduct(String productId);
}
