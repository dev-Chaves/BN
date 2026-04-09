package org.acme.domains.auth;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.shared.api.BaseResource;
import org.acme.domains.shared.api.IpResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource implements BaseResource {

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthResource(
            AuthService authService,
            @ConfigProperty(name = "app.cookie.secure", defaultValue = "true") boolean cookieSecure
    ) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @POST()
    @Path("/login")
    @PermitAll
    @RateLimited(bucket = "auth-group", identityResolver = IpResolver.class)
    public Uni<Response> login(@Valid LoginRequest request) {
        return authService.login(request)
                .map(this::toLoginResponseWithCookie);
    }

    private Response toLoginResponseWithCookie(LoginResponse loginResponse) {
        NewCookie cookie = new NewCookie.Builder("jwt")
                .value(loginResponse.token())
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();

        return Response.ok(loginResponse)
                .cookie(cookie)
                .build();
    }

    public <T> Uni<Response> toCreated(Uni<T> useCaseResult) {
        return BaseResource.super.toCreated(useCaseResult);
    }

    public <T> Uni<Response> toOk(Uni<T> useCaseResult) {
        return BaseResource.super.toOk(useCaseResult);
    }

    public <T> Uni<Response> delete(Uni<T> useCaseResult) {
        return BaseResource.super.delete(useCaseResult);
    }
}
