package com.bnfix.ubm.domains.manager;

import com.bnfix.ubm.domains.manager.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/managers")
public class ManagerController {
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateManagerRequest request) {
        log.info("POST /managers for email {}", request.email());
        return ResponseEntity.status(201).body(managerService.create(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(managerService.me(email(jwt), company(jwt)));
    }

    @PutMapping("/me/email")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> email(
            @Valid @RequestBody UpdateManagerEmailRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /managers/me/email by {}", email(jwt));
        return ResponseEntity.ok(managerService.updateEmail(email(jwt), company(jwt), request));
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> password(
            @Valid @RequestBody ChangeManagerPasswordRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /managers/me/password by {}", email(jwt));
        return ResponseEntity.ok(managerService.changePassword(email(jwt), company(jwt), request));
    }

    private String email(Jwt jwt) {
        return jwt.getClaimAsString("email") != null ? jwt.getClaimAsString("email") : jwt.getSubject();
    }

    private Long company(Jwt jwt) {
        return com.bnfix.ubm.shared.security.JwtCompanyContext.requireCompanyId(jwt);
    }
}
