package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.PharmaRolesDto;
import tiameds.pharmabackend.service.PharmaRolesService;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class PharmaRolesController {

    private final PharmaRolesService pharmaRolesService;

    @GetMapping
    public ResponseEntity<List<PharmaRolesDto>> getAllRoles() {
        return ResponseEntity.ok(pharmaRolesService.getAllRoles());
    }
}
