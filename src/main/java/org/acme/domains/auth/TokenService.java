package org.acme.domains.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    public String generateToken(String email, Long companyId, String role) {

        return Jwt.issuer("bn-api")
                .upn(email)
                .subject(email)
                .claim("companyId", companyId)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(3))
                .sign();
    }

}
