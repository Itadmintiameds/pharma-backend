package tiameds.pharmabackend.service.product;

import tiameds.pharmabackend.dto.product.ProductDetailsDto;

public interface ProductService {
    ProductDetailsDto onboardProduct(ProductDetailsDto dto);
    java.util.List<ProductDetailsDto> getAllProducts();
    ProductDetailsDto getProductById(String productId);
    void deleteProduct(String productId);
}
