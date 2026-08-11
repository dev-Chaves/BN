package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.auth.dto.AuthMeResponse;
import com.bnfix.ubm.domains.auth.dto.AuthResult;
import com.bnfix.ubm.domains.auth.dto.LoginRequest;
import com.bnfix.ubm.domains.auth.dto.SwitchCompanyRequest;
import com.bnfix.ubm.shared.security.RevokedTokenStore;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final AuthMeService authMeService;
    private final RevokedTokenStore revokedTokenStore;
    private final boolean cookieSecure;
    private final String cookieDomain;

    public AuthController(
            AuthService authService,
            SwitchCompanyService switchCompanyService,
            AuthMeService authMeService,
            RevokedTokenStore revokedTokenStore,
            @Value("${app.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.cookie.domain:}") String cookieDomain) {
        this.authService = authService;
        this.switchCompanyService = switchCompanyService;
        this.authMeService = authMeService;
        this.revokedTokenStore = revokedTokenStore;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain == null ? "" : cookieDomain.trim();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthMeResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email {}", request.email());
        return tokenResponse(authService.login(request));
    }

    @PostMapping("/switch-company")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AuthMeResponse> switchCompany(
            @Valid @RequestBody SwitchCompanyRequest request, Authentication authentication) {
        log.info("Company switch requested by {} to company {}", authentication.getName(), request.companyId());
        return tokenResponse(switchCompanyService.switchCompany(authentication.getName(), request.companyId()));
    }

    /**
     * Invalida imediatamente a sessão: adiciona o {@code jti} atual à blocklist
     * (revogação antecipada) e expira o cookie no browser.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(Authentication authentication) {
        var jwt = ((JwtAuthenticationToken) authentication).getToken();
        String jti = jwt.getClaimAsString("jti");
        if (jti != null) revokedTokenStore.revoke(UUID.fromString(jti), jwt.getExpiresAt());
        log.info("User {} logged out", authentication.getName());
        return ResponseEntity.noContent()
                .header("Set-Cookie", expiredCookie().toString())
                .header("Cache-Control", "no-store")
                .build();
    }

    /**
     * Perfil do usuário autenticado — usado pelo frontend para restaurar a
     * sessão no reload (o cookie httpOnly não é legível por JS).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
        var jwt = ((JwtAuthenticationToken) authentication).getToken();
        String email = jwt.getClaimAsString("email") != null ? jwt.getClaimAsString("email") : jwt.getSubject();
        String role = firstGroup(jwt);
        Long companyId = companyId(jwt);
        return ResponseEntity.ok(authMeService.build(email, role, companyId));
    }

    private ResponseEntity<AuthMeResponse> tokenResponse(AuthResult result) {
        return ResponseEntity.ok()
                .header("Set-Cookie", sessionCookie(result.token()).toString())
                .header("Cache-Control", "no-store")
                .body(result.user());
    }

    private ResponseCookie sessionCookie(String token) {
        var builder = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Strict")
                .maxAge(SESSION_SECONDS);
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }

    private ResponseCookie expiredCookie() {
        var builder = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Strict")
                .maxAge(0);
        if (!cookieDomain.isBlank()) builder.domain(cookieDomain);
        return builder.build();
    }

    private String firstGroup(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        return groups == null || groups.isEmpty() ? null : groups.get(0);
    }

    private Long companyId(Jwt jwt) {
        Object claim = jwt.getClaims().get("companyId");
        if (claim == null) return null;
        return claim instanceof Number number ? number.longValue() : Long.valueOf(claim.toString());
    }
}
