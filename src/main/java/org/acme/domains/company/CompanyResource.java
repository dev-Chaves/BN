package org.acme.domains.company;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.company.dto.CreateCompanyRequest;
import org.acme.domains.company.dto.DeactivateCompanyRequest;
import org.acme.domains.shared.api.BaseResource;
import org.acme.domains.shared.security.JwtCompanyContext;
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
    @RolesAllowed("MANAGER")
    public Uni<Response> listMine() {
        return toOk(companyService.listActiveByManagerEmail(jwt.getName()));
    }

    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> create(@Valid CreateCompanyRequest request) {
        return toCreated(companyService.createForCurrentAccount(request, jwt.getName()));
    }

    @GET
    @Path("/me")
    @RolesAllowed("MANAGER")
    public Uni<Response> myCompany() {
        return toOk(companyService.getByManagerEmailAndCompanyId(
                jwt.getName(),
                JwtCompanyContext.requireCompanyId(jwt)
        ));
    }

    @PUT
    @Path("/me/deactivate")
    @RolesAllowed("MANAGER")
    public Uni<Response> deactivateMine(@Valid DeactivateCompanyRequest request) {
        return toOk(companyService.deactivateCurrent(
                jwt.getName(),
                JwtCompanyContext.requireCompanyId(jwt),
                request
        ));
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
