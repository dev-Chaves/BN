package org.acme.domains.sharedbenefit;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/shared-benefits")
@Produces(MediaType.APPLICATION_JSON)
public class SharedBenefitResource {
    private final SharedBenefitService service;
    private final JsonWebToken jwt;

    public SharedBenefitResource(SharedBenefitService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GET
    @Path("/available")
    @RolesAllowed("USER")
    public Uni<Response> available() {
        return service.available(jwt.getName()).map(items -> Response.ok(items).build());
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    public Uni<Response> mine() {
        return service.mine(jwt.getName()).map(items -> Response.ok(items).build());
    }
}
