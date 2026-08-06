package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.PurchaseSmallestUnitDto;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.master.PurchaseSmallestUnit;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.repository.master.PurchaseSmallestUnitRepository;
import tiameds.pharmabackend.service.master.PurchaseSmallestUnitService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseSmallestUnitServiceImpl implements PurchaseSmallestUnitService {

    private final PurchaseSmallestUnitRepository purchaseSmallestUnitRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseSmallestUnitDto> getAllPurchaseSmallestUnits() {
        return purchaseSmallestUnitRepository
                .findAll(Sort.by("purchaseSmallestUnitId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseSmallestUnitDto getPurchaseSmallestUnitById(Long purchaseSmallestUnitId) {
        return toDto(findPurchaseSmallestUnit(purchaseSmallestUnitId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseSmallestUnitDto> getPurchaseSmallestUnitsByCategoryId(Long productCategoryId) {

        if (!productCategoryRepository.existsById(productCategoryId)) {
            throw new RuntimeException("Product category not found with id: " + productCategoryId);
        }

        return purchaseSmallestUnitRepository
                .findByProductCategory_ProductCategoryId(productCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PurchaseSmallestUnitDto createPurchaseSmallestUnit(PurchaseSmallestUnitDto purchaseSmallestUnitDto) {

        PurchaseSmallestUnit purchaseSmallestUnit = new PurchaseSmallestUnit();
        purchaseSmallestUnit.setPurchaseSmallestUnitName(purchaseSmallestUnitDto.getPurchaseSmallestUnitName());
        purchaseSmallestUnit.setPurchaseUnitName(purchaseSmallestUnitDto.getPurchaseUnitName());

        if (purchaseSmallestUnitDto.getProductCategoryId() != null) {
            purchaseSmallestUnit.setProductCategory(findProductCategory(purchaseSmallestUnitDto.getProductCategoryId()));
        }

        purchaseSmallestUnit.setIsActive(purchaseSmallestUnitDto.getIsActive() != null ? purchaseSmallestUnitDto.getIsActive() : true);
        purchaseSmallestUnit.setCreatedAt(LocalDateTime.now());

        return toDto(purchaseSmallestUnitRepository.save(purchaseSmallestUnit));
    }

    @Override
    public PurchaseSmallestUnitDto updatePurchaseSmallestUnit(Long purchaseSmallestUnitId, PurchaseSmallestUnitDto purchaseSmallestUnitDto) {

        PurchaseSmallestUnit purchaseSmallestUnit = findPurchaseSmallestUnit(purchaseSmallestUnitId);

        purchaseSmallestUnit.setPurchaseSmallestUnitName(purchaseSmallestUnitDto.getPurchaseSmallestUnitName());
        purchaseSmallestUnit.setPurchaseUnitName(purchaseSmallestUnitDto.getPurchaseUnitName());

        if (purchaseSmallestUnitDto.getProductCategoryId() != null) {
            purchaseSmallestUnit.setProductCategory(findProductCategory(purchaseSmallestUnitDto.getProductCategoryId()));
        }

        if (purchaseSmallestUnitDto.getIsActive() != null) {
            purchaseSmallestUnit.setIsActive(purchaseSmallestUnitDto.getIsActive());
        }
        purchaseSmallestUnit.setModifiedAt(LocalDateTime.now());

        return toDto(purchaseSmallestUnitRepository.save(purchaseSmallestUnit));
    }

    @Override
    public PurchaseSmallestUnitDto updatePurchaseSmallestUnitStatus(Long purchaseSmallestUnitId, Boolean isActive) {

        PurchaseSmallestUnit purchaseSmallestUnit = findPurchaseSmallestUnit(purchaseSmallestUnitId);

        purchaseSmallestUnit.setIsActive(isActive);
        purchaseSmallestUnit.setModifiedAt(LocalDateTime.now());

        return toDto(purchaseSmallestUnitRepository.save(purchaseSmallestUnit));
    }

    private PurchaseSmallestUnit findPurchaseSmallestUnit(Long purchaseSmallestUnitId) {
        return purchaseSmallestUnitRepository.findById(purchaseSmallestUnitId)
                .orElseThrow(() -> new RuntimeException("Purchase smallest unit not found with id: " + purchaseSmallestUnitId));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Product category not found with id: " + productCategoryId));
    }

    private PurchaseSmallestUnitDto toDto(PurchaseSmallestUnit purchaseSmallestUnit) {
        PurchaseSmallestUnitDto dto = new PurchaseSmallestUnitDto();
        dto.setPurchaseSmallestUnitId(purchaseSmallestUnit.getPurchaseSmallestUnitId());
        dto.setPurchaseSmallestUnitName(purchaseSmallestUnit.getPurchaseSmallestUnitName());
        dto.setPurchaseUnitName(purchaseSmallestUnit.getPurchaseUnitName());

        if (purchaseSmallestUnit.getProductCategory() != null) {
            dto.setProductCategoryId(purchaseSmallestUnit.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(purchaseSmallestUnit.getProductCategory().getProductCategoryName());
        }

        dto.setIsActive(purchaseSmallestUnit.getIsActive());
        return dto;
    }
}
