package org.acme.domains.partnership;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.partnership.dto.CreatePartnershipRequest;
import org.acme.domains.shared.api.BaseResource;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@Path("/partnerships")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PartnershipResource implements BaseResource {

    private final PartnershipService partnershipService;
    private final JsonWebToken jwt;

    public PartnershipResource(PartnershipService partnershipService, JsonWebToken jwt) {
        this.partnershipService = partnershipService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> request(@Valid CreatePartnershipRequest request) {
        return toCreated(partnershipService.requestPartnership(jwt.getName(), request.benefitId()));
    }

    @PUT
    @Path("/accept")
    @RolesAllowed("MANAGER")
    public Uni<Response> accept(@QueryParam("partnershipId") Long partnershipId) {
        return toOk(partnershipService.acceptPartnership(jwt.getName(), partnershipId));
    }

    @PUT
    @Path("/reject")
    @RolesAllowed("MANAGER")
    public Uni<Response> reject(@QueryParam("partnershipId") Long partnershipId) {
        return toOk(partnershipService.rejectPartnership(jwt.getName(), partnershipId));
    }

    @PUT
    @Path("/disable")
    @RolesAllowed("MANAGER")
    public Uni<Response> disable(@QueryParam("partnershipId") Long partnershipId) {
        return toOk(partnershipService.disablePartnership(jwt.getName(), partnershipId));
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
