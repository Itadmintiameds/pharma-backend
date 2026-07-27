package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.ProductFormDto;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.repository.master.ProductFormRepository;
import tiameds.pharmabackend.service.master.ProductFormService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductFormServiceImpl implements ProductFormService {

    private final ProductFormRepository productFormRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductFormDto> getAllProductForms() {
        return productFormRepository
                .findAll(Sort.by("productFormId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductFormDto getProductFormById(Long productFormId) {
        return toDto(findProductForm(productFormId));
    }

    @Override
    public ProductFormDto createProductForm(ProductFormDto productFormDto) {

        ProductForm productForm = new ProductForm();
        productForm.setProductFormName(productFormDto.getProductFormName());
        productForm.setIsActive(productFormDto.getIsActive() != null ? productFormDto.getIsActive() : true);
        productForm.setCreatedAt(LocalDateTime.now());

        return toDto(productFormRepository.save(productForm));
    }

    @Override
    public ProductFormDto updateProductForm(Long productFormId, ProductFormDto productFormDto) {

        ProductForm productForm = findProductForm(productFormId);

        productForm.setProductFormName(productFormDto.getProductFormName());
        if (productFormDto.getIsActive() != null) {
            productForm.setIsActive(productFormDto.getIsActive());
        }
        productForm.setModifiedAt(LocalDateTime.now());

        return toDto(productFormRepository.save(productForm));
    }

    @Override
    public ProductFormDto updateProductFormStatus(Long productFormId, Boolean isActive) {

        ProductForm productForm = findProductForm(productFormId);

        productForm.setIsActive(isActive);
        productForm.setModifiedAt(LocalDateTime.now());

        return toDto(productFormRepository.save(productForm));
    }

    private ProductForm findProductForm(Long productFormId) {
        return productFormRepository.findById(productFormId)
                .orElseThrow(() -> new RuntimeException("Product form not found with id: " + productFormId));
    }

    private ProductFormDto toDto(ProductForm productForm) {
        ProductFormDto dto = new ProductFormDto();
        dto.setProductFormId(productForm.getProductFormId());
        dto.setProductFormName(productForm.getProductFormName());
        dto.setIsActive(productForm.getIsActive());
        return dto;
    }
}
