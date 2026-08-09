package com.bnfix.ubm.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider provider;

    public JwtCookieAuthenticationFilter(JwtAuthenticationProvider provider) {
        this.provider = provider;
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
                SecurityContextHolder.getContext().setAuthentication(authentication);
                var jwt = authentication.getToken();
                Object companyClaim = jwt.getClaims().get("companyId");
                Long companyId = companyClaim instanceof Number number
                        ? number.longValue()
                        : companyClaim == null ? null : Long.valueOf(companyClaim.toString());
                TenantContext.set(companyId, jwt.getClaimAsString("email"), jwt.getClaimAsString("accountId"));
            }
            chain.doFilter(request, response);
        } catch (org.springframework.security.core.AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String bearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        return header.substring(7).trim();
    }
}
