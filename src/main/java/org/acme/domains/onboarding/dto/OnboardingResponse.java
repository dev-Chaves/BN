package org.acme.domains.onboarding.dto;

public record OnboardingResponse(
        String cnpj,
        String company,
        String manager
) {
}
