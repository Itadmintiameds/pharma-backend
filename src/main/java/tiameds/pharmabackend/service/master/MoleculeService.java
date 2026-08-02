package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.MoleculeDto;

import java.util.List;

public interface MoleculeService {

    List<MoleculeDto> getAllMolecules();

    MoleculeDto getMoleculeById(Long moleculeId);

    MoleculeDto createMolecule(MoleculeDto moleculeDto);

    MoleculeDto updateMolecule(Long moleculeId, MoleculeDto moleculeDto);

    MoleculeDto updateMoleculeStatus(Long moleculeId, Boolean isActive);
}
