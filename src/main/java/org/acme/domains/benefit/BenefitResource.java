package org.acme.domains.benefit;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.shared.api.BaseResource;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
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
    public Uni<Response> createBenefit(CreateBenefitRequest request){
        String email = jwt.getName();

        return toCreated(benefitService.createBenefit(request, email));
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
