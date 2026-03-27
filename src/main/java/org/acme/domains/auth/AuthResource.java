package org.acme.domains.auth;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.shared.api.BaseResource;

@ApplicationScoped
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource implements BaseResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST()
    @Path("/login")
    @PermitAll
    public Uni<Response> login(@Valid LoginRequest request){

        return toOk(authService.login(request));

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
