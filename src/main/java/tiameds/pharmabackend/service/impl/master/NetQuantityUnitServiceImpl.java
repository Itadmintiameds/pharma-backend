package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.NetQuantityUnitDto;
import tiameds.pharmabackend.entity.master.NetQuantityUnit;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.repository.master.NetQuantityUnitRepository;
import tiameds.pharmabackend.repository.master.ProductCategoryRepository;
import tiameds.pharmabackend.service.master.NetQuantityUnitService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NetQuantityUnitServiceImpl implements NetQuantityUnitService {

    private final NetQuantityUnitRepository netQuantityUnitRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NetQuantityUnitDto> getAllNetQuantityUnits() {
        return netQuantityUnitRepository
                .findAll(Sort.by("netQuantityUnitId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NetQuantityUnitDto getNetQuantityUnitById(Long netQuantityUnitId) {
        return toDto(findNetQuantityUnit(netQuantityUnitId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetQuantityUnitDto> getNetQuantityUnitsByCategoryId(Long productCategoryId) {

        if (!productCategoryRepository.existsById(productCategoryId)) {
            throw new RuntimeException("Product category not found with id: " + productCategoryId);
        }

        return netQuantityUnitRepository
                .findByProductCategory_ProductCategoryId(productCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public NetQuantityUnitDto createNetQuantityUnit(NetQuantityUnitDto netQuantityUnitDto) {

        NetQuantityUnit netQuantityUnit = new NetQuantityUnit();
        netQuantityUnit.setNetQuantityUnitName(netQuantityUnitDto.getNetQuantityUnitName());

        if (netQuantityUnitDto.getProductCategoryId() != null) {
            netQuantityUnit.setProductCategory(
                    findProductCategory(netQuantityUnitDto.getProductCategoryId()));
        }

        netQuantityUnit.setIsActive(
                netQuantityUnitDto.getIsActive() != null ? netQuantityUnitDto.getIsActive() : true);
        netQuantityUnit.setCreatedAt(LocalDateTime.now());

        return toDto(netQuantityUnitRepository.save(netQuantityUnit));
    }

    @Override
    public NetQuantityUnitDto updateNetQuantityUnit(Long netQuantityUnitId,
                                                    NetQuantityUnitDto netQuantityUnitDto) {

        NetQuantityUnit netQuantityUnit = findNetQuantityUnit(netQuantityUnitId);

        netQuantityUnit.setNetQuantityUnitName(netQuantityUnitDto.getNetQuantityUnitName());

        if (netQuantityUnitDto.getProductCategoryId() != null) {
            netQuantityUnit.setProductCategory(
                    findProductCategory(netQuantityUnitDto.getProductCategoryId()));
        }

        if (netQuantityUnitDto.getIsActive() != null) {
            netQuantityUnit.setIsActive(netQuantityUnitDto.getIsActive());
        }
        netQuantityUnit.setModifiedAt(LocalDateTime.now());

        return toDto(netQuantityUnitRepository.save(netQuantityUnit));
    }

    @Override
    public NetQuantityUnitDto updateNetQuantityUnitStatus(Long netQuantityUnitId, Boolean isActive) {

        NetQuantityUnit netQuantityUnit = findNetQuantityUnit(netQuantityUnitId);

        netQuantityUnit.setIsActive(isActive);
        netQuantityUnit.setModifiedAt(LocalDateTime.now());

        return toDto(netQuantityUnitRepository.save(netQuantityUnit));
    }

    private NetQuantityUnit findNetQuantityUnit(Long netQuantityUnitId) {
        return netQuantityUnitRepository.findById(netQuantityUnitId)
                .orElseThrow(() -> new RuntimeException(
                        "Net quantity unit not found with id: " + netQuantityUnitId));
    }

    private ProductCategory findProductCategory(Long productCategoryId) {
        return productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new RuntimeException(
                        "Product category not found with id: " + productCategoryId));
    }

    private NetQuantityUnitDto toDto(NetQuantityUnit netQuantityUnit) {
        NetQuantityUnitDto dto = new NetQuantityUnitDto();
        dto.setNetQuantityUnitId(netQuantityUnit.getNetQuantityUnitId());
        dto.setNetQuantityUnitName(netQuantityUnit.getNetQuantityUnitName());

        if (netQuantityUnit.getProductCategory() != null) {
            dto.setProductCategoryId(netQuantityUnit.getProductCategory().getProductCategoryId());
            dto.setProductCategoryName(netQuantityUnit.getProductCategory().getProductCategoryName());
        }

        dto.setIsActive(netQuantityUnit.getIsActive());
        return dto;
    }
}
