package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.DosageFormDto;
import tiameds.pharmabackend.entity.master.DosageForm;
import tiameds.pharmabackend.repository.master.DosageFormRepository;
import tiameds.pharmabackend.service.master.DosageFormService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DosageFormServiceImpl implements DosageFormService {

    private final DosageFormRepository dosageFormRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DosageFormDto> getAllDosageForms() {
        return dosageFormRepository
                .findAll(Sort.by("dosageId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DosageFormDto getDosageFormById(Long dosageId) {
        return toDto(findDosageForm(dosageId));
    }

    @Override
    public DosageFormDto createDosageForm(DosageFormDto dosageFormDto) {

        DosageForm dosageForm = new DosageForm();
        dosageForm.setDosageName(dosageFormDto.getDosageName());
        dosageForm.setIsActive(dosageFormDto.getIsActive() != null ? dosageFormDto.getIsActive() : true);
        dosageForm.setCreatedAt(LocalDateTime.now());

        return toDto(dosageFormRepository.save(dosageForm));
    }

    @Override
    public DosageFormDto updateDosageForm(Long dosageId, DosageFormDto dosageFormDto) {

        DosageForm dosageForm = findDosageForm(dosageId);

        dosageForm.setDosageName(dosageFormDto.getDosageName());
        if (dosageFormDto.getIsActive() != null) {
            dosageForm.setIsActive(dosageFormDto.getIsActive());
        }
        dosageForm.setModifiedAt(LocalDateTime.now());

        return toDto(dosageFormRepository.save(dosageForm));
    }

    @Override
    public DosageFormDto updateDosageFormStatus(Long dosageId, Boolean isActive) {

        DosageForm dosageForm = findDosageForm(dosageId);

        dosageForm.setIsActive(isActive);
        dosageForm.setModifiedAt(LocalDateTime.now());

        return toDto(dosageFormRepository.save(dosageForm));
    }

    private DosageForm findDosageForm(Long dosageId) {
        return dosageFormRepository.findById(dosageId)
                .orElseThrow(() -> new RuntimeException("Dosage form not found with id: " + dosageId));
    }

    private DosageFormDto toDto(DosageForm dosageForm) {
        DosageFormDto dto = new DosageFormDto();
        dto.setDosageId(dosageForm.getDosageId());
        dto.setDosageName(dosageForm.getDosageName());
        dto.setIsActive(dosageForm.getIsActive());
        return dto;
    }
}
