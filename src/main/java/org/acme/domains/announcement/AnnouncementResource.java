package org.acme.domains.announcement;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.announcement.dto.CreateAnnouncementRequest;
import org.acme.domains.shared.security.JwtCompanyContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/announcements")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AnnouncementResource {
    private final AnnouncementService service;
    private final JsonWebToken jwt;

    public AnnouncementResource(AnnouncementService service, JsonWebToken jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> publish(@Valid CreateAnnouncementRequest request) {
        return service.publish(jwt.getName(), companyId(), request)
                .map(item -> Response.status(Response.Status.CREATED).entity(item).build());
    }

    @GET
    @Path("/company")
    @RolesAllowed("MANAGER")
    public Uni<Response> listCompany(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        return service.listCompany(jwt.getName(), companyId(), page, size)
                .map(item -> Response.ok(item).build());
    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    public Uni<Response> listMine(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        return service.listMine(jwt.getName(), companyId(), page, size)
                .map(item -> Response.ok(item).build());
    }

    @GET
    @Path("/me/unread-count")
    @RolesAllowed("USER")
    public Uni<Response> unreadCount() {
        return service.unreadCount(jwt.getName(), companyId())
                .map(item -> Response.ok(item).build());
    }

    @PUT
    @Path("/{announcementId}/read")
    @RolesAllowed("USER")
    public Uni<Response> markRead(@PathParam("announcementId") Long announcementId) {
        return service.markRead(jwt.getName(), companyId(), announcementId)
                .map(item -> Response.ok(item).build());
    }

    @PUT
    @Path("/me/read-all")
    @RolesAllowed("USER")
    public Uni<Response> markAllAsRead() {
        return service.markAllAsRead(jwt.getName(), companyId())
                .map(item -> Response.ok(item).build());
    }

    private Long companyId() {
        return JwtCompanyContext.requireCompanyId(jwt);
    }
}
