package org.acme.domains.employee;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.JsonNumber;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
import org.acme.domains.employee.dto.UpdateEmployeeRequest;
import org.acme.domains.shared.api.BaseResource;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class EmployeeResource implements BaseResource {

    private final JsonWebToken jwt;

    private final EmployeeService employeeService;

    public EmployeeResource(JsonWebToken jwt, EmployeeService employeeService) {
        this.jwt = jwt;
        this.employeeService = employeeService;
    }

    @POST
    @RolesAllowed("MANAGER")
    public Uni<Response> createEmployee(@Valid CreateEmployeeRequest request){

           String email = jwt.getName();

           return toCreated(employeeService.createEmployee(request,email, request.companyId()));

    }

    @PUT
    @Path("/disable")
    @RolesAllowed("MANAGER")
    public Uni<Response> disabledEmployee(@QueryParam("employeeId") Long employeeId){

        String email = jwt.getName();

        return toOk(employeeService.disabledEmployee(employeeId, email));
    }

    @PUT
    @Path("/activate")
    @RolesAllowed("MANAGER")
    public Uni<Response> activateEmployee(@QueryParam("employeeId") Long employeeId){
        return toOk(employeeService.activateEmployee(employeeId, jwt.getName()));
    }

    @PUT
    @Path("/{employeeId}")
    @RolesAllowed("MANAGER")
    public Uni<Response> updateEmployee(@PathParam("employeeId") Long employeeId, @Valid UpdateEmployeeRequest request){
        return toOk(employeeService.updateEmployee(employeeId, request, jwt.getName()));
    }

    @GET
    @RolesAllowed("MANAGER")
    public Uni<Response> listByTenant(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("50") int size) {
        return toOk(employeeService.listByTenant(jwt.getName(), claimCompanyId(), page, size));
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
