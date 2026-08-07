package com.bnfix.ubm.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtCompanyContext {
    private JwtCompanyContext() {}
    public static Long requireCompanyId(Jwt jwt) {
        Object claim = jwt.getClaims().get("companyId");
        if (claim instanceof Number number) return number.longValue();
        if (claim instanceof String value) try { return Long.parseLong(value); } catch (NumberFormatException ignored) { }
        throw new IllegalStateException("Missing or invalid companyId claim");
    }
}
