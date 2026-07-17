package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.repository.PharmaRolesRepository;
import tiameds.pharmabackend.service.PharmaRolesService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PharmaRolesServiceImpl implements PharmaRolesService {

    private final PharmaRolesRepository pharmaRolesRepository;

    @Override
    public List<PharmaRolesDto> getAllRoles() {

        return pharmaRolesRepository
                .findAll(Sort.by("roleId"))
                .stream()
                .map(role -> {
                    PharmaRolesDto dto = new PharmaRolesDto();
                    dto.setRoleId(role.getRoleId());
                    dto.setRoleName(role.getRoleName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
