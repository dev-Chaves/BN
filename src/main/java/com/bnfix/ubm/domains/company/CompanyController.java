package com.bnfix.ubm.domains.company;

import com.bnfix.ubm.domains.company.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listMine(email(jwt)));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateCompanyRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(service.create(request, email(jwt)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getMine(email(jwt), company(jwt)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> update(
            @Valid @RequestBody UpdateCompanyRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.update(email(jwt), company(jwt), request));
    }

    @PutMapping("/me/deactivate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deactivate(
            @Valid @RequestBody DeactivateCompanyRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.deactivate(email(jwt), company(jwt), request));
    }

    private String email(Jwt jwt) {
        return jwt.getClaimAsString("email") != null ? jwt.getClaimAsString("email") : jwt.getSubject();
    }

    private Long company(Jwt jwt) {
        return com.bnfix.ubm.shared.security.JwtCompanyContext.requireCompanyId(jwt);
    }
}
