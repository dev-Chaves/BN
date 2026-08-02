package org.acme.domains.shared.security;

import jakarta.json.JsonNumber;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Reads the tenant selected in a signed JWT without depending on the JSON
 * representation chosen by the JWT provider.
 */
public final class JwtCompanyContext {
    private JwtCompanyContext() {
    }

    public static Long requireCompanyId(JsonWebToken jwt) {
        Object claim = jwt.claim("companyId").orElse(null);
        if (claim instanceof Long value) {
            return value;
        }
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof JsonNumber jsonNumber) {
            return jsonNumber.longValue();
        }
        if (claim instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // Fall through to the consistent error below.
            }
        }
        throw new IllegalStateException("Missing or invalid companyId claim");
    }
}
