package tiameds.pharmabackend.controller.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.dto.legal.TermsPolicyAdminDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.TermsPolicyService;

/**
 * Publishing side of the terms &amp; privacy policy.
 * <p>
 * No @PreAuthorize: any authenticated user can publish a version. SecurityConfig's
 * anyRequest().authenticated() is the only gate here.
 */
@RestController
@RequestMapping("/admin/terms")
@RequiredArgsConstructor
public class AdminTermsPolicyController {

    private final TermsPolicyService termsPolicyService;

    @GetMapping
    public ResponseEntity<List<TermsPolicyAdminDto>> listAll() {
        return ResponseEntity.ok(termsPolicyService.listAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TermsPolicyAdminDto> upload(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("version") String version,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("effectiveFrom")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effectiveFrom,
            @RequestParam(value = "requiresReacceptance", defaultValue = "false")
            boolean requiresReacceptance) {

        TermsPolicyAdminDto response = termsPolicyService.upload(
                file,
                version,
                title,
                effectiveFrom,
                requiresReacceptance,
                currentUser == null ? null : currentUser.getUserId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<TermsPolicyAdminDto> publish(@PathVariable Long id) {
        return ResponseEntity.ok(termsPolicyService.publish(id));
    }
}
