package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.MaterialTypeDto;
import tiameds.pharmabackend.entity.master.MaterialType;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.repository.master.MaterialTypeRepository;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.service.master.MaterialTypeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MaterialTypeServiceImpl implements MaterialTypeService {

    private final MaterialTypeRepository materialTypeRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MaterialTypeDto> getAllMaterialTypes() {
        return materialTypeRepository
                .findAll(Sort.by("materialTypeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialTypeDto getMaterialTypeById(Long materialTypeId) {
        return toDto(findMaterialType(materialTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialTypeDto> getMaterialTypesByCategoryId(Long productCategoryId) {

        if (!productCategoryRepository.existsById(productCategoryId)) {
            throw new RuntimeException("Product category not found with id: " + productCategoryId);
        }

        return materialTypeRepository
                .findByProductCategory_ProductCategoryId(productCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MaterialTypeDto createMaterialType(MaterialTypeDto materialTypeDto) {

        MaterialType materialType = new MaterialType();
        materialType.setMaterialTypeName(materialTypeDto.getMaterialTypeName());
        if (materialTypeDto.getProductCategoryId() != null) {
            materialType.setProductCategory(findProductCategory(materialTypeDto.getProductCategoryId()));
        }
        materialType.setIsActive(materialTypeDto.getIsActive() != null ? materialTypeDto.getIsActive() : true);
        materialType.setCreatedAt(LocalDateTime.now());

        return toDto(materialTypeRepository.save(materialType));
    }

    @Override
    public MaterialTypeDto updateMaterialType(Long materialTypeId, MaterialTypeDto materialTypeDto) {

        MaterialType materialType = findMaterialType(materialTypeId);

        materialType.setMaterialTypeName(materialTypeDto.getMaterialTypeName());
        if (materialTypeDto.getProductCategoryId() != null) {
            materialType.setProductCategory(findProductCategory(materialTypeDto.getProductCategoryId()));
        }
        if (materialTypeDto.getIsActive() != null) {
            materialType.setIsActive(materialTypeDto.getIsActive());
        }
        materialType.setModifiedAt(LocalDateTime.now());

        return toDto(materialTypeRepository.save(materialType));
    }

    @Override
    public MaterialTypeDto updateMaterialTypeStatus(Long materialTypeId, Boolean isActive) {

        MaterialType materialType = findMaterialType(materialTypeId);

        materialType.setIsActive(isActive);
        materialType.setModifiedAt(LocalDateTime.now());

        return toDto(materialTypeRepository.save(materialType));
    }

    private MaterialType findMaterialType(Long materialTypeId) {
        return materialTypeRepository.findById(materialTypeId)
                .orElseThrow(() -> new RuntimeException("Material type not found with id: " + materialTypeId));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException("Product category not found with id: " + productCategoryId));
    }

    private MaterialTypeDto toDto(MaterialType materialType) {
        MaterialTypeDto dto = new MaterialTypeDto();
        dto.setMaterialTypeId(materialType.getMaterialTypeId());
        dto.setMaterialTypeName(materialType.getMaterialTypeName());

        if (materialType.getProductCategory() != null) {
            dto.setProductCategoryId(materialType.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(materialType.getProductCategory().getProductCategoryName());
        }

        dto.setIsActive(materialType.getIsActive());
        return dto;
    }
}
