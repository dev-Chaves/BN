package org.acme.domains.manager;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.manager.dto.CreateManagerRequest;
import org.acme.domains.shared.api.BaseResource;
import org.acme.domains.shared.security.JwtCompanyContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@Path("/managers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ManagerResource implements BaseResource {

    private final ManagerService managerService;
    private final JsonWebToken jwt;

    public ManagerResource(ManagerService managerService, JsonWebToken jwt) {
        this.managerService = managerService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("ADMIN")
    public Uni<Response> create(@Valid CreateManagerRequest request) {
        return toCreated(managerService.createManager(request));
    }

    @GET
    @Path("/me")
    @RolesAllowed("MANAGER")
    public Uni<Response> me() {
        return toOk(managerService.getCurrentManager(jwt.getName(), JwtCompanyContext.requireCompanyId(jwt)));
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
