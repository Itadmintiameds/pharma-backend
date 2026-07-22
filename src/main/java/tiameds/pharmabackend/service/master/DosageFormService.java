package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.DosageFormDto;

import java.util.List;

public interface DosageFormService {

    List<DosageFormDto> getAllDosageForms();

    DosageFormDto getDosageFormById(Long dosageId);

    DosageFormDto createDosageForm(DosageFormDto dosageFormDto);

    DosageFormDto updateDosageForm(Long dosageId, DosageFormDto dosageFormDto);

    DosageFormDto updateDosageFormStatus(Long dosageId, Boolean isActive);
}
