package org.acme.domains.onboarding.dto;

public record OnboardingResponse(
        Long companyId,
        Long managerId,
        String cnpj,
        String nameCompany,
        String nameManager
) {
}
