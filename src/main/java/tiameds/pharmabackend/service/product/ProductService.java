package tiameds.pharmabackend.service.product;

import tiameds.pharmabackend.dto.product.ProductDetailResponseDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.ProductStockSummaryDto;

import java.util.List;

public interface ProductService {
    ProductDetailsDto onboardProduct(ProductDetailsDto dto);
    java.util.List<ProductDetailsDto> getAllProducts();
    ProductDetailsDto getProductById(String productId);
    void deleteProduct(String productId);

    // API 1: all products of the current pharmacy with stock + expiry status
    List<ProductStockSummaryDto> getProductStockSummaries();

    // API 2: complete details of one product with batches grouped per package
    ProductDetailResponseDto getProductDetails(String productId);
}
