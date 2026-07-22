package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.FlavourDto;

import java.util.List;

public interface FlavourService {

    List<FlavourDto> getAllFlavours();

    FlavourDto getFlavourById(Long flavourId);

    FlavourDto createFlavour(FlavourDto flavourDto);

    FlavourDto updateFlavour(Long flavourId, FlavourDto flavourDto);

    FlavourDto updateFlavourStatus(Long flavourId, Boolean isActive);
}
