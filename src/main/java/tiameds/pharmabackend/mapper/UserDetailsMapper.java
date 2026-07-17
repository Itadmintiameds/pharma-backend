package tiameds.pharmabackend.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserDetailsMapper {

    private final PharmacyDetailsMapper pharmacyDetailsMapper;

    public UserDetails toEntity(UserDetailsDto dto) {

        UserDetails user = new UserDetails();

        user.setUserId(dto.getUserId());
        user.setPassword(dto.getPassword());
        user.setUserEmail(dto.getUserEmail());
        user.setFullName(dto.getFullName());
        user.setUserPhone(dto.getUserPhone());
        user.setEmployeeId(dto.getEmployeeId());
        user.setDob(dto.getDob());
        user.setGender(dto.getGender());
        user.setDepartment(dto.getDepartment());
        user.setImageUrl(dto.getImageUrl());
        user.setUserStatus(dto.getUserStatus());
        user.setCreatedAt(dto.getCreatedAt());
        user.setModifiedBy(dto.getModifiedBy());
        user.setModifiedAt(dto.getModifiedAt());

        return user;
    }

    public UserDetails toEntity(UserDetailsDto dto, PharmaRoles role) {

        UserDetails user = toEntity(dto);
        user.setRole(role);

        return user;
    }

    public UserDetailsDto toDto(UserDetails user) {

        UserDetailsDto dto = new UserDetailsDto();

        dto.setUserId(user.getUserId());
        dto.setUserEmail(user.getUserEmail());
        dto.setFullName(user.getFullName());
        dto.setUserPhone(user.getUserPhone());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setDob(user.getDob());
        dto.setGender(user.getGender());
        dto.setDepartment(user.getDepartment());
        dto.setImageUrl(user.getImageUrl());
        dto.setUserStatus(user.getUserStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setModifiedBy(user.getModifiedBy());
        dto.setModifiedAt(user.getModifiedAt());

        if (user.getRole() != null) {

            PharmaRolesDto roleDto = new PharmaRolesDto();

            roleDto.setRoleId(user.getRole().getRoleId());
            roleDto.setRoleName(user.getRole().getRoleName());

            dto.setPharmaRolesDto(roleDto);
        }

        if (user.getPharmacies() != null && !user.getPharmacies().isEmpty()) {
            dto.setPharmacies(
                    user.getPharmacies()
                            .stream()
                            .map(pharmacyDetailsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    public UserSummaryDto toSummaryDto(UserDetails user) {

        UserSummaryDto dto = new UserSummaryDto();

        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setUserEmail(user.getUserEmail());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setUserStatus(user.getUserStatus());

        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getRoleId());
            dto.setRoleName(user.getRole().getRoleName());
        }

        if (user.getPharmacies() != null) {
            dto.setPharmacyCities(
                    user.getPharmacies()
                            .stream()
                            .map(PharmacyDetails::getPharmacyCity)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}