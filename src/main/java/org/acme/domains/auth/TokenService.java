package org.acme.domains.auth;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    public String generateToken(String email, Long companyId, String role) {

        JwtClaimsBuilder builder = Jwt.issuer("bn-api")
                .upn(email)
                .subject(email)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(3));
        if (companyId != null) {
            builder.claim("companyId", companyId);
        }
        return builder.sign();
    }

}
