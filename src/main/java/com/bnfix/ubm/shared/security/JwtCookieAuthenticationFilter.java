package com.bnfix.ubm.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider provider;
    private final RevokedTokenStore revokedTokenStore;

    public JwtCookieAuthenticationFilter(JwtAuthenticationProvider provider, RevokedTokenStore revokedTokenStore) {
        this.provider = provider;
        this.revokedTokenStore = revokedTokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = bearer(request.getHeader("Authorization"));
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies())
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
        }
        try {
            if (token != null && !token.isBlank()) {
                JwtAuthenticationToken authentication =
                        (JwtAuthenticationToken) provider.authenticate(new BearerTokenAuthenticationToken(token));
                var jwt = authentication.getToken();
                if (isRevoked(jwt)) {
                    log.warn("Revoked token rejected for {} {}", request.getMethod(), request.getRequestURI());
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
                Object companyClaim = jwt.getClaims().get("companyId");
                Long companyId = companyClaim instanceof Number number
                        ? number.longValue()
                        : companyClaim == null ? null : Long.valueOf(companyClaim.toString());
                TenantContext.set(companyId, jwt.getClaimAsString("email"), jwt.getClaimAsString("accountId"));
            }
            chain.doFilter(request, response);
        } catch (org.springframework.security.core.AuthenticationException exception) {
            log.warn("Rejected token for {} {}", request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isRevoked(org.springframework.security.oauth2.jwt.Jwt jwt) {
        String jti = jwt.getClaimAsString("jti");
        if (jti == null) return false;
        return revokedTokenStore.isRevoked(UUID.fromString(jti), jwt.getExpiresAt());
    }

    private String bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        return header.substring(7).trim();
    }
}
