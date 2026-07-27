package org.acme.domains.redemption;

import io.smallrye.mutiny.Uni;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.redemption.dto.RedemptionTokenRequest;
import org.acme.domains.shared.api.IpResolver;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/redemptions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RedemptionResource {
    private final RedemptionService service;
    private final JsonWebToken jwt;

    public RedemptionResource(RedemptionService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @POST
    @Path("/subscriptions/{subscriptionId}/token")
    @RolesAllowed("USER")
    @RateLimited(bucket = "redemption-group", identityResolver = IpResolver.class)
    public Uni<Response> issue(@PathParam("subscriptionId") Long subscriptionId) {
        return service.issue(jwt.getName(), subscriptionId)
                .map(item -> Response.status(Response.Status.CREATED).entity(item).build());
    }

    @POST
    @Path("/provider/preview")
    @RolesAllowed("MANAGER")
    @RateLimited(bucket = "redemption-group", identityResolver = IpResolver.class)
    public Uni<Response> preview(@Valid RedemptionTokenRequest request) {
        return service.preview(jwt.getName(), request.token()).map(item -> Response.ok(item).build());
    }

    @POST
    @Path("/provider/consume")
    @RolesAllowed("MANAGER")
    @RateLimited(bucket = "redemption-group", identityResolver = IpResolver.class)
    public Uni<Response> consume(@Valid RedemptionTokenRequest request) {
        return service.consume(jwt.getName(), request.token()).map(item -> Response.ok(item).build());
    }
}
