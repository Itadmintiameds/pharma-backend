package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.HairTypeDto;
import tiameds.pharmabackend.entity.master.HairType;
import tiameds.pharmabackend.repository.master.HairTypeRepository;
import tiameds.pharmabackend.service.master.HairTypeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class HairTypeServiceImpl implements HairTypeService {

    private final HairTypeRepository hairTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HairTypeDto> getAllHairTypes() {
        return hairTypeRepository
                .findAll(Sort.by("hairTypeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HairTypeDto getHairTypeById(Long hairTypeId) {
        return toDto(findHairType(hairTypeId));
    }

    @Override
    public HairTypeDto createHairType(HairTypeDto hairTypeDto) {

        HairType hairType = new HairType();
        hairType.setHairTypeName(hairTypeDto.getHairTypeName());
        hairType.setIsActive(hairTypeDto.getIsActive() != null ? hairTypeDto.getIsActive() : true);
        hairType.setCreatedAt(LocalDateTime.now());

        return toDto(hairTypeRepository.save(hairType));
    }

    @Override
    public HairTypeDto updateHairType(Long hairTypeId, HairTypeDto hairTypeDto) {

        HairType hairType = findHairType(hairTypeId);

        hairType.setHairTypeName(hairTypeDto.getHairTypeName());
        if (hairTypeDto.getIsActive() != null) {
            hairType.setIsActive(hairTypeDto.getIsActive());
        }
        hairType.setModifiedAt(LocalDateTime.now());

        return toDto(hairTypeRepository.save(hairType));
    }

    @Override
    public HairTypeDto updateHairTypeStatus(Long hairTypeId, Boolean isActive) {

        HairType hairType = findHairType(hairTypeId);

        hairType.setIsActive(isActive);
        hairType.setModifiedAt(LocalDateTime.now());

        return toDto(hairTypeRepository.save(hairType));
    }

    private HairType findHairType(Long hairTypeId) {
        return hairTypeRepository.findById(hairTypeId)
                .orElseThrow(() -> new RuntimeException("Hair type not found with id: " + hairTypeId));
    }

    private HairTypeDto toDto(HairType hairType) {
        HairTypeDto dto = new HairTypeDto();
        dto.setHairTypeId(hairType.getHairTypeId());
        dto.setHairTypeName(hairType.getHairTypeName());
        dto.setIsActive(hairType.getIsActive());
        return dto;
    }
}
