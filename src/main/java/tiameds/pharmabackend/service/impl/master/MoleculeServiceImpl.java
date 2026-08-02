package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.MoleculeDto;
import tiameds.pharmabackend.entity.master.Molecule;
import tiameds.pharmabackend.repository.master.MoleculeRepository;
import tiameds.pharmabackend.service.master.MoleculeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MoleculeServiceImpl implements MoleculeService {

    private final MoleculeRepository moleculeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MoleculeDto> getAllMolecules() {
        return moleculeRepository
                .findAll(Sort.by("moleculeId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MoleculeDto getMoleculeById(Long moleculeId) {
        return toDto(findMolecule(moleculeId));
    }

    @Override
    public MoleculeDto createMolecule(MoleculeDto moleculeDto) {

        Molecule molecule = new Molecule();
        molecule.setMoleculeName(moleculeDto.getMoleculeName());
        molecule.setDrugSchedule(moleculeDto.getDrugSchedule());
        molecule.setIsActive(moleculeDto.getIsActive() != null ? moleculeDto.getIsActive() : true);
        molecule.setCreatedAt(LocalDateTime.now());

        return toDto(moleculeRepository.save(molecule));
    }

    @Override
    public MoleculeDto updateMolecule(Long moleculeId, MoleculeDto moleculeDto) {

        Molecule molecule = findMolecule(moleculeId);

        molecule.setMoleculeName(moleculeDto.getMoleculeName());
        molecule.setDrugSchedule(moleculeDto.getDrugSchedule());
        if (moleculeDto.getIsActive() != null) {
            molecule.setIsActive(moleculeDto.getIsActive());
        }
        molecule.setModifiedAt(LocalDateTime.now());

        return toDto(moleculeRepository.save(molecule));
    }

    @Override
    public MoleculeDto updateMoleculeStatus(Long moleculeId, Boolean isActive) {

        Molecule molecule = findMolecule(moleculeId);

        molecule.setIsActive(isActive);
        molecule.setModifiedAt(LocalDateTime.now());

        return toDto(moleculeRepository.save(molecule));
    }

    private Molecule findMolecule(Long moleculeId) {
        return moleculeRepository.findById(moleculeId)
                .orElseThrow(() -> new RuntimeException("Molecule not found with id: " + moleculeId));
    }

    private MoleculeDto toDto(Molecule molecule) {
        MoleculeDto dto = new MoleculeDto();
        dto.setMoleculeId(molecule.getMoleculeId());
        dto.setMoleculeName(molecule.getMoleculeName());
        dto.setDrugSchedule(molecule.getDrugSchedule());
        dto.setIsActive(molecule.getIsActive());
        return dto;
    }
}
