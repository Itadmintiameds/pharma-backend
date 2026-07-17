package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.PermissionDto;

import java.util.List;

public interface PermissionService {

    List<PermissionDto> getAllPermissions();
}
