package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.ProductSubTypeDto;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;
import tiameds.pharmabackend.repository.master.ProductSubTypeRepository;
import tiameds.pharmabackend.repository.master.ProductTypeRepository;
import tiameds.pharmabackend.service.master.ProductSubTypeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductSubTypeServiceImpl implements ProductSubTypeService {

    private final ProductSubTypeRepository productSubTypeRepository;
    private final ProductTypeRepository productTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductSubTypeDto> getAllProductSubTypes() {
        return productSubTypeRepository
                .findAll(Sort.by("productSubTypeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSubTypeDto getProductSubTypeById(Long productSubTypeId) {
        return toDto(findProductSubType(productSubTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSubTypeDto> getProductSubTypesByTypeId(Long productTypeId) {

        if (!productTypeRepository.existsById(productTypeId)) {
            throw new RuntimeException("Product type not found with id: " + productTypeId);
        }

        return productSubTypeRepository
                .findByProductType_ProductTypeId(productTypeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductSubTypeDto createProductSubType(ProductSubTypeDto productSubTypeDto) {

        ProductType productType = findProductType(productSubTypeDto.getProductTypeId());

        ProductSubType productSubType = new ProductSubType();
        productSubType.setProductSubTypeName(productSubTypeDto.getProductSubTypeName());
        productSubType.setProductType(productType);
        productSubType.setIsActive(
                productSubTypeDto.getIsActive() != null ? productSubTypeDto.getIsActive() : true);
        productSubType.setCreatedAt(LocalDateTime.now());

        return toDto(productSubTypeRepository.save(productSubType));
    }

    @Override
    public ProductSubTypeDto updateProductSubType(Long productSubTypeId,
                                                  ProductSubTypeDto productSubTypeDto) {

        ProductSubType productSubType = findProductSubType(productSubTypeId);

        productSubType.setProductSubTypeName(productSubTypeDto.getProductSubTypeName());

        if (productSubTypeDto.getProductTypeId() != null) {
            productSubType.setProductType(findProductType(productSubTypeDto.getProductTypeId()));
        }

        if (productSubTypeDto.getIsActive() != null) {
            productSubType.setIsActive(productSubTypeDto.getIsActive());
        }
        productSubType.setModifiedAt(LocalDateTime.now());

        return toDto(productSubTypeRepository.save(productSubType));
    }

    @Override
    public ProductSubTypeDto updateProductSubTypeStatus(Long productSubTypeId, Boolean isActive) {

        ProductSubType productSubType = findProductSubType(productSubTypeId);

        productSubType.setIsActive(isActive);
        productSubType.setModifiedAt(LocalDateTime.now());

        return toDto(productSubTypeRepository.save(productSubType));
    }

    private ProductSubType findProductSubType(Long productSubTypeId) {
        return productSubTypeRepository.findById(productSubTypeId)
                .orElseThrow(() -> new RuntimeException(
                        "Product sub type not found with id: " + productSubTypeId));
    }

    private ProductType findProductType(Long productTypeId) {

        if (productTypeId == null) {
            throw new RuntimeException("Product type id is required");
        }

        return productTypeRepository.findById(productTypeId)
                .orElseThrow(() -> new RuntimeException("Product type not found with id: " + productTypeId));
    }

    private ProductSubTypeDto toDto(ProductSubType productSubType) {
        ProductSubTypeDto dto = new ProductSubTypeDto();
        dto.setProductSubTypeId(productSubType.getProductSubTypeId());
        dto.setProductSubTypeName(productSubType.getProductSubTypeName());

        if (productSubType.getProductType() != null) {
            dto.setProductTypeId(productSubType.getProductType().getProductTypeId());
            dto.setProductTypeName(productSubType.getProductType().getProductTypeName());
        }

        dto.setIsActive(productSubType.getIsActive());
        return dto;
    }
}
