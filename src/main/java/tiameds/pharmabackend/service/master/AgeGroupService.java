package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.AgeGroupDto;

import java.util.List;

public interface AgeGroupService {

    List<AgeGroupDto> getAllAgeGroups();

    AgeGroupDto getAgeGroupById(Long ageGroupId);

    AgeGroupDto createAgeGroup(AgeGroupDto ageGroupDto);

    AgeGroupDto updateAgeGroup(Long ageGroupId, AgeGroupDto ageGroupDto);

    AgeGroupDto updateAgeGroupStatus(Long ageGroupId, Boolean isActive);
}
