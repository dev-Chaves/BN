package com.bnfix.ubm.domains.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration expiration;

    public TokenService(
            JwtEncoder encoder,
            @Value("${app.jwt.issuer:bn-api}") String issuer,
            @Value("${app.jwt.expiration:PT3H}") Duration expiration) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public String generateToken(String email, UUID accountId, Long companyId, String role) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .subject(email)
                .claim("upn", email)
                .claim("email", email)
                .claim("groups", List.of(role));
        if (accountId != null) claims.claim("accountId", accountId.toString());
        if (companyId != null) claims.claim("companyId", companyId);
        log.debug(
                "Generated JWT for email {} (role={}, accountId={}, companyId={})", email, role, accountId, companyId);
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims.build()))
                .getTokenValue();
    }

    public String generateToken(String email, Long companyId, String role) {
        return generateToken(email, null, companyId, role);
    }
}
