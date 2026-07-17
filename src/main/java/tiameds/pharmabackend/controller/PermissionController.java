package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.PermissionDto;
import tiameds.pharmabackend.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }
}
