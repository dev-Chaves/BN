package com.bnfix.ubm.domains.manager;

import com.bnfix.ubm.domains.manager.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/managers")
public class ManagerController {
    private final ManagerService service;

    public ManagerController(ManagerService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateManagerRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.me(email(j), company(j)));
    }

    @PutMapping("/me/email")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> email(@Valid @RequestBody UpdateManagerEmailRequest r, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.updateEmail(email(j), company(j), r));
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> password(
            @Valid @RequestBody ChangeManagerPasswordRequest r, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.changePassword(email(j), company(j), r));
    }

    private String email(Jwt j) {
        return j.getClaimAsString("email") != null ? j.getClaimAsString("email") : j.getSubject();
    }

    private Long company(Jwt j) {
        return com.bnfix.ubm.shared.security.JwtCompanyContext.requireCompanyId(j);
    }
}
