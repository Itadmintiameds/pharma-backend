package tiameds.pharmabackend.mapper;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.UserDetails;

@Component
public class UserDetailsMapper {


    public UserDetails toEntity(UserDetailsDto dto) {

        UserDetails user = new UserDetails();

        user.setUserId(dto.getUserId());
        user.setUserName(dto.getUserName());
        user.setPassword(dto.getPassword());
        user.setUserEmail(dto.getUserEmail());

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
        dto.setUserName(user.getUserName());
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

        return dto;
    }
}