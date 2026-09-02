package tiameds.pharmabackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.dto.legal.TermsPolicyDto;
import tiameds.pharmabackend.service.TermsPolicyService;

/**
 * Public, unauthenticated access to the terms &amp; privacy policy — the
 * registration screen has to render it before an account exists.
 * Whitelisted as /terms/** in SecurityConfig.
 */
@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsPolicyController {

    private final TermsPolicyService termsPolicyService;

    @GetMapping("/current")
    public ResponseEntity<TermsPolicyDto> getCurrent() {
        return ResponseEntity.ok(termsPolicyService.getCurrent());
    }

    @GetMapping("/versions/{version}")
    public ResponseEntity<TermsPolicyDto> getByVersion(@PathVariable String version) {
        return ResponseEntity.ok(termsPolicyService.getByVersion(version));
    }
}
