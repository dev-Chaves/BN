package org.acme.domains.subscription;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domains.shared.api.BaseResource;
import org.acme.domains.subscription.dto.CreateSubscriptionRequest;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@Path("/subscriptions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubscriptionResource implements BaseResource {

    private final SubscriptionService subscriptionService;
    private final JsonWebToken jwt;

    public SubscriptionResource(SubscriptionService subscriptionService, JsonWebToken jwt) {
        this.subscriptionService = subscriptionService;
        this.jwt = jwt;
    }

    @POST
    @RolesAllowed("USER")
    public Uni<Response> subscribe(@Valid CreateSubscriptionRequest request) {
        return toCreated(subscriptionService.subscribeToBenefit(request, jwt.getName()));
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
