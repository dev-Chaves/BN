package org.acme.domains.benefit;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.JsonNumber;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.benefit.dto.UpdateBenefitRequest;
import org.acme.domains.shared.api.BaseResource;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@Path("/benefits")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BenefitResource implements BaseResource {

    private final BenefitService benefitService;

    private final JsonWebToken jwt;

    public BenefitResource(BenefitService benefitService, JsonWebToken jwt) {
        this.benefitService = benefitService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> createBenefit(@Valid CreateBenefitRequest request){
        String email = jwt.getName();

        return toCreated(benefitService.createBenefit(request, email));
    }

    @GET
    @Path("/tenant")
    @RolesAllowed("MANAGER")
    public Uni<Response> listBenefitsByTenant() {
        return toOk(benefitService.listBenefitsByTenant(claimCompanyId(), jwt.getName()));
    }

    @GET
    @Path("/marketplace")
    @RolesAllowed("MANAGER")
    public Uni<Response> managerMarketplace() {
        return toOk(benefitService.managerMarketplace(jwt.getName()));
    }

    @PUT
    @Path("/{benefitId}")
    @RolesAllowed("MANAGER")
    public Uni<Response> updateBenefit(@PathParam("benefitId") Long benefitId, @Valid UpdateBenefitRequest request) {
        return toOk(benefitService.updateBenefit(benefitId, request, jwt.getName()));
    }

    @PUT
    @Path("/{benefitId}/activate")
    @RolesAllowed("MANAGER")
    public Uni<Response> activateBenefit(@PathParam("benefitId") Long benefitId) {
        return toOk(benefitService.activateBenefit(benefitId, jwt.getName()));
    }

    @PUT
    @Path("/{benefitId}/deactivate")
    @RolesAllowed("MANAGER")
    public Uni<Response> deactivateBenefit(@PathParam("benefitId") Long benefitId) {
        return toOk(benefitService.deactivateBenefit(benefitId, jwt.getName()));
    }

    @DELETE
    @Path("/{benefitId}")
    @RolesAllowed("MANAGER")
    public Uni<Response> deleteBenefit(@PathParam("benefitId") Long benefitId) {
        return delete(benefitService.deleteBenefit(benefitId, jwt.getName()));
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

    private Long claimCompanyId() {
        try {
            return jwt.getClaim("companyId");
        } catch (ClassCastException ignored) {
            Object claim = jwt.claim("companyId").orElse(null);
            if (claim instanceof Number number) {
                return number.longValue();
            }
            if (claim instanceof JsonNumber jsonNumber) {
                return jsonNumber.longValue();
            }
            if (claim instanceof String value) {
                return Long.parseLong(value);
            }
        }
        throw new IllegalStateException("Invalid companyId claim type");
    }
}
