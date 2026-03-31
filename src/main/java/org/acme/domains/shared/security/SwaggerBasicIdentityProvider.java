package org.acme.domains.shared.security;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ApplicationScoped
public class SwaggerBasicIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private final String expectedUsername;
    private final String expectedPassword;

    public SwaggerBasicIdentityProvider(
            @ConfigProperty(name = "app.swagger.auth.username", defaultValue = "") String expectedUsername,
            @ConfigProperty(name = "app.swagger.auth.password", defaultValue = "") String expectedPassword
    ) {
        if (LaunchMode.current() == LaunchMode.NORMAL && (expectedUsername.isBlank() || expectedPassword.isBlank())) {
            throw new IllegalStateException("Missing Swagger Basic Auth credentials: define SWAGGER_BASIC_AUTH_USERNAME and SWAGGER_BASIC_AUTH_PASSWORD");
        }
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
    }

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
        String receivedPassword = new String(request.getPassword().getPassword());
        if (!equalsConstantTime(expectedUsername, request.getUsername()) || !equalsConstantTime(expectedPassword, receivedPassword)) {
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }

        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(expectedUsername))
                .addRole("SWAGGER_DOCS")
                .addCredential(request.getPassword())
                .build();
        return Uni.createFrom().item(identity);
    }

    private boolean equalsConstantTime(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
