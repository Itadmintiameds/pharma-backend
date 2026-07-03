package tiameds.pharmabackend.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
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
}