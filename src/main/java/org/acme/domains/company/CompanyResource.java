package org.acme.domains.company;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.shared.api.BaseResource;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@Path("/companies")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CompanyResource implements BaseResource {

    private final CompanyService companyService;
    private final JsonWebToken jwt;

    public CompanyResource(CompanyService companyService, JsonWebToken jwt) {
        this.companyService = companyService;
        this.jwt = jwt;
    }

    @GET
    @Path("/me")
    @RolesAllowed("MANAGER")
    public Uni<Response> myCompany() {
        return toOk(companyService.getByManagerEmail(jwt.getName()));
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
