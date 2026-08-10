package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.auth.dto.LoginRequest;
import com.bnfix.ubm.domains.auth.dto.LoginResponse;
import com.bnfix.ubm.domains.auth.dto.SwitchCompanyRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Profile("!test")
@RequestMapping("/auth")
public class AuthController {
    private static final int SESSION_SECONDS = 3 * 60 * 60;
    private final AuthService authService;
    private final SwitchCompanyService switchCompanyService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            SwitchCompanyService switchCompanyService,
            @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.authService = authService;
        this.switchCompanyService = switchCompanyService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email {}", request.email());
        return tokenResponse(authService.login(request));
    }

    @PostMapping("/switch-company")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LoginResponse> switchCompany(
            @Valid @RequestBody SwitchCompanyRequest request, Authentication authentication) {
        log.info("Company switch requested by {} to company {}", authentication.getName(), request.companyId());
        return tokenResponse(switchCompanyService.switchCompany(authentication.getName(), request.companyId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("User logged out");
        return ResponseEntity.noContent()
                .header("Set-Cookie", expiredCookie().toString())
                .header("Cache-Control", "no-store")
                .build();
    }

    private ResponseEntity<LoginResponse> tokenResponse(LoginResponse response) {
        return ResponseEntity.ok()
                .header("Set-Cookie", sessionCookie(response.token()).toString())
                .header("Cache-Control", "no-store")
                .body(response);
    }

    private ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Strict")
                .maxAge(SESSION_SECONDS)
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }
}
