package org.acme.domains.benefitrequest;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.benefitrequest.dto.CreateBenefitAccessRequest;
import org.acme.domains.benefitrequest.dto.RejectBenefitAccessRequest;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/benefit-requests")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BenefitAccessRequestResource {
    private final BenefitAccessRequestService service;
    private final JsonWebToken jwt;

    public BenefitAccessRequestResource(BenefitAccessRequestService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("USER")
    public Uni<Response> request(@Valid CreateBenefitAccessRequest request) {
        return service.request(jwt.getName(), request.benefitId())
                .map(item -> Response.status(Response.Status.CREATED).entity(item).build());
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    public Uni<Response> mine() {
        return service.mine(jwt.getName()).map(item -> Response.ok(item).build());
    }

    @GET
    @Path("/provider")
    @RolesAllowed("MANAGER")
    public Uni<Response> providerPending() {
        return service.providerPending(jwt.getName()).map(item -> Response.ok(item).build());
    }

    @PUT
    @Path("/{requestId}/approve")
    @RolesAllowed("MANAGER")
    public Uni<Response> approve(@PathParam("requestId") Long requestId) {
        return service.approve(jwt.getName(), requestId).map(item -> Response.ok(item).build());
    }

    @PUT
    @Path("/{requestId}/reject")
    @RolesAllowed("MANAGER")
    public Uni<Response> reject(@PathParam("requestId") Long requestId, @Valid RejectBenefitAccessRequest request) {
        return service.reject(jwt.getName(), requestId, request.reason()).map(item -> Response.ok(item).build());
    }
}
