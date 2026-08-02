package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.SkinTypeDto;
import tiameds.pharmabackend.entity.master.SkinType;
import tiameds.pharmabackend.repository.master.SkinTypeRepository;
import tiameds.pharmabackend.service.master.SkinTypeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SkinTypeServiceImpl implements SkinTypeService {

    private final SkinTypeRepository skinTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SkinTypeDto> getAllSkinTypes() {
        return skinTypeRepository
                .findAll(Sort.by("skinTypeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SkinTypeDto getSkinTypeById(Long skinTypeId) {
        return toDto(findSkinType(skinTypeId));
    }

    @Override
    public SkinTypeDto createSkinType(SkinTypeDto skinTypeDto) {

        SkinType skinType = new SkinType();
        skinType.setSkinTypeName(skinTypeDto.getSkinTypeName());
        skinType.setIsActive(skinTypeDto.getIsActive() != null ? skinTypeDto.getIsActive() : true);
        skinType.setCreatedAt(LocalDateTime.now());

        return toDto(skinTypeRepository.save(skinType));
    }

    @Override
    public SkinTypeDto updateSkinType(Long skinTypeId, SkinTypeDto skinTypeDto) {

        SkinType skinType = findSkinType(skinTypeId);

        skinType.setSkinTypeName(skinTypeDto.getSkinTypeName());
        if (skinTypeDto.getIsActive() != null) {
            skinType.setIsActive(skinTypeDto.getIsActive());
        }
        skinType.setModifiedAt(LocalDateTime.now());

        return toDto(skinTypeRepository.save(skinType));
    }

    @Override
    public SkinTypeDto updateSkinTypeStatus(Long skinTypeId, Boolean isActive) {

        SkinType skinType = findSkinType(skinTypeId);

        skinType.setIsActive(isActive);
        skinType.setModifiedAt(LocalDateTime.now());

        return toDto(skinTypeRepository.save(skinType));
    }

    private SkinType findSkinType(Long skinTypeId) {
        return skinTypeRepository.findById(skinTypeId)
                .orElseThrow(() -> new RuntimeException("Skin type not found with id: " + skinTypeId));
    }

    private SkinTypeDto toDto(SkinType skinType) {
        SkinTypeDto dto = new SkinTypeDto();
        dto.setSkinTypeId(skinType.getSkinTypeId());
        dto.setSkinTypeName(skinType.getSkinTypeName());
        dto.setIsActive(skinType.getIsActive());
        return dto;
    }
}
