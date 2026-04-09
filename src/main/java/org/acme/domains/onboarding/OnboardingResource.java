package org.acme.domains.onboarding;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.acme.domains.onboarding.dto.OnboardingResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.acme.domains.shared.api.BaseResource;
import org.acme.domains.shared.api.IpResolver;

@Path("/onboarding")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OnboardingResource implements BaseResource {

    private final OnboardingService onboardingService;

    public OnboardingResource(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @POST
    @RateLimited(bucket = "onboarding-group", identityResolver = IpResolver.class)
    public Uni<Response> onboardingCompany(@Valid OnboardingRequest request) {
        return toCreated(onboardingService.onboardingCompany(request));
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
