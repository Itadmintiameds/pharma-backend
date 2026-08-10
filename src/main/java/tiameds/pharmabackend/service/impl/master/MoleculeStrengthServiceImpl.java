package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.ModuleStrengthDto;
import tiameds.pharmabackend.entity.master.MoleculeStrength;
import tiameds.pharmabackend.repository.master.MoleculeStrengthRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MoleculeStrengthServiceImpl {

    private final MoleculeStrengthRepository moleculeStrengthRepository;

    public List<ModuleStrengthDto> getAllMoleculeStrength() {

        return moleculeStrengthRepository
                .findAll(Sort.by(Sort.Direction.ASC, "moleculeStrengthId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ModuleStrengthDto toDto(MoleculeStrength moleculeStrength) {

        ModuleStrengthDto dto = new ModuleStrengthDto();

        dto.setMoleculeStrengthId(moleculeStrength.getMoleculeStrengthId());
        dto.setMoleculeStrengthName(moleculeStrength.getMoleculeStrengthName());

        return dto;
    }
}