package org.acme.domains.onboarding;

import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.acme.domains.onboarding.dto.OnboardingResponse;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/onboarding")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OnboardingResource {

    private final OnboardingService onboardingService;

    public OnboardingResource(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @POST
    public Uni<OnboardingResponse> onboardingCompany(OnboardingRequest request) {
        return onboardingService.onboardingCompany(request);
    }

}
