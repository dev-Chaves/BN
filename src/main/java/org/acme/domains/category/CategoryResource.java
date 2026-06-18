package org.acme.domains.category;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.shared.api.BaseResource;

import java.util.List;

@ApplicationScoped
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource implements BaseResource {

    private final CategoryService categoryService;

    public CategoryResource(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GET
    @RolesAllowed("MANAGER")
    public Uni<Response> listAll() {
        return BaseResource.super.toOk(categoryService.listAll());
    }
}
