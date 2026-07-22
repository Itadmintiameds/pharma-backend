package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.FlavourDto;
import tiameds.pharmabackend.entity.master.Flavour;
import tiameds.pharmabackend.repository.master.FlavourRepository;
import tiameds.pharmabackend.service.master.FlavourService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FlavourServiceImpl implements FlavourService {

    private final FlavourRepository flavourRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FlavourDto> getAllFlavours() {
        return flavourRepository
                .findAll(Sort.by("flavourId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FlavourDto getFlavourById(Long flavourId) {
        return toDto(findFlavour(flavourId));
    }

    @Override
    public FlavourDto createFlavour(FlavourDto flavourDto) {

        if (flavourRepository.existsByFlavourNameIgnoreCase(flavourDto.getFlavourName())) {
            throw new RuntimeException("Flavour already exists with name: " + flavourDto.getFlavourName());
        }

        Flavour flavour = new Flavour();
        flavour.setFlavourName(flavourDto.getFlavourName());
        flavour.setIsActive(flavourDto.getIsActive() != null ? flavourDto.getIsActive() : true);
        flavour.setCreatedAt(LocalDateTime.now());

        return toDto(flavourRepository.save(flavour));
    }

    @Override
    public FlavourDto updateFlavour(Long flavourId, FlavourDto flavourDto) {

        Flavour flavour = findFlavour(flavourId);

        flavour.setFlavourName(flavourDto.getFlavourName());
        if (flavourDto.getIsActive() != null) {
            flavour.setIsActive(flavourDto.getIsActive());
        }
        flavour.setModifiedAt(LocalDateTime.now());

        return toDto(flavourRepository.save(flavour));
    }

    @Override
    public FlavourDto updateFlavourStatus(Long flavourId, Boolean isActive) {

        Flavour flavour = findFlavour(flavourId);

        flavour.setIsActive(isActive);
        flavour.setModifiedAt(LocalDateTime.now());

        return toDto(flavourRepository.save(flavour));
    }

    private Flavour findFlavour(Long flavourId) {
        return flavourRepository.findById(flavourId)
                .orElseThrow(() -> new RuntimeException("Flavour not found with id: " + flavourId));
    }

    private FlavourDto toDto(Flavour flavour) {
        FlavourDto dto = new FlavourDto();
        dto.setFlavourId(flavour.getFlavourId());
        dto.setFlavourName(flavour.getFlavourName());
        dto.setIsActive(flavour.getIsActive());
        return dto;
    }
}
