package tiameds.pharmabackend.service.product;

import tiameds.pharmabackend.dto.product.AddPackageRequest;
import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.BatchStockDto;
import tiameds.pharmabackend.dto.product.ProductDetailResponseDto;
import tiameds.pharmabackend.dto.product.ProductDetailsDto;
import tiameds.pharmabackend.dto.product.ProductExpiryKpiDto;
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

    // Dashboard KPI: product counts bucketed by nearest in-stock expiry
    ProductExpiryKpiDto getExpiryKpi();

    // Add a new package (optionally with batches) to an existing product
    ProductDetailResponseDto addPackage(String productId, AddPackageRequest request);

    // Add one or more batches to existing packages of a product (each batch carries its packagingId)
    ProductDetailResponseDto addBatches(String productId, List<BatchDetailsDto> batches);

    // All batches of the current pharmacy with product, packaging, stock and pricing
    List<BatchStockDto> getAllBatches();

    // One batch with the same detail as the listing
    BatchStockDto getBatchById(String batchId);
}
