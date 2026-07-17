package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.PermissionDto;
import tiameds.pharmabackend.repository.PharmaPermissionRepository;
import tiameds.pharmabackend.service.PermissionService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PharmaPermissionRepository pharmaPermissionRepository;

    @Override
    public List<PermissionDto> getAllPermissions() {

        return pharmaPermissionRepository
                .findAll(Sort.by("permissionId"))
                .stream()
                .map(permission -> {
                    PermissionDto dto = new PermissionDto();
                    dto.setPermissionId(permission.getPermissionId());
                    dto.setPermissionName(permission.getPermissionName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
