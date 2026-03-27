package org.acme.domains.onboarding.dto;

public record OnboardingResponse(
        String cnpj,
        String nameCompany,
        String nameManager
) {
}
