package org.acme.domains.employee;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.employee.dto.CreateEmployeeRequest;
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

           return toCreated(employeeService.createEmployee(request,email));

    }

    @PUT
    @RolesAllowed("MANAGER")
    public Uni<Response> disabledEmployee(@QueryParam("employeeId") Long employeeId){

        return toOk(employeeService.disabledEmployee(employeeId));
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
